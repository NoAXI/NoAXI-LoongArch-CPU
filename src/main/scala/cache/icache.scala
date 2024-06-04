package axi

import chisel3._
import chisel3.util._

import cache._
import bundles._
import Funcs.Functions._
import const.cacheConst._
import const.Parameters._

class iCacheIO extends Bundle {
  val axi   = new iCache_AXI
  val fetch = Flipped(new fetch_iCache_IO)
}

class iCache extends Module {
  val io = IO(new iCacheIO)

  // 2^8 line * 16 B/line = 4KB
  val datasram = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val tagsram  = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first(TAG_WIDTH, LINE_WIDTH)).io)
  val validreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))

  val qaddr   = RegInit(0.U(ADDR_WIDTH.W))
  val qtag    = RegInit(0.U(TAG_WIDTH.W))
  val qindex  = RegInit(0.U(8.W))
  val qoffset = RegInit(0.U(4.W))

  val valid   = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val dirty   = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val wdata   = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask   = RegInit(1.U(4.W))
  val hit     = WireDefault(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val hitdata = WireDefault(0.U(DATA_WIDTH.W))

  // lru
  val lru = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(3)(true.B)))))
  def updatelru(index: UInt, way: UInt): Unit = {
    switch(way) {
      is(0.U) { lru(index)(0) := true.B }
      is(1.U) { lru(index)(0) := false.B }
      is(2.U) { lru(index)(1) := true.B }
      is(3.U) { lru(index)(1) := false.B }
    }
    lru(index)(2) := Mux(way(1), false.B, true.B)
  }
  def waychosen(index: UInt): UInt = {
    Mux(
      lru(index)(2),
      Mux(lru(index)(1), 3.U, 2.U),
      Mux(lru(index)(0), 1.U, 0.U),
    )
  }

  val ar          = RegInit(0.U.asTypeOf(new AR))
  val arvalid     = RegInit(false.B)
  val rready      = RegInit(false.B)
  val ans_valid   = RegInit(false.B)
  val ans_bits    = RegInit(0.U(INST_WIDTH.W))
  val req_ready   = RegInit(true.B)
  val stall       = RegInit(false.B)
  val saved_valid = RegInit(false.B)
  val saved_inst  = RegInit(0.U(INST_WIDTH.W))
  val saved_query = RegInit(false.B)
  val saved_addr  = RegInit(0.U(32.W))

  val addr     = Mux(io.fetch.request.valid, io.fetch.request.bits, saved_addr)
  val ram_addr = addr(11, 4)
  for (i <- 0 until WAY_WIDTH) {
    datasram(i).clka  := clock
    datasram(i).addra := ram_addr
    datasram(i).wea   := false.B
    datasram(i).dina  := 0.U
    tagsram(i).clka   := clock
    tagsram(i).addra  := ram_addr
    tagsram(i).wea    := false.B
    tagsram(i).dina   := 0.U
  }

  val cached = true.B
  ar.id := 0.U
  // ar.len   := 0.U
  ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  val _qindex = addr(11, 4)
  qaddr   := addr
  qtag    := addr(31, 12)
  qindex  := _qindex
  qoffset := addr(3, 0)
  for (i <- 0 until WAY_WIDTH) {
    valid(i) := validreg(_qindex)(i)
  }

  for (i <- 0 until WAY_WIDTH) {
    when(valid(i) && tagsram(i).douta === qtag) {
      hit(i) := true.B
    }
  }

  for (i <- 0 until WAY_WIDTH) {
    val data = datasram(i).douta
    when(hit(i)) {
      updatelru(qindex, i.U)
      hitdata := MateDefault(
        qoffset(3, 2),
        0.U,
        Seq(
          0.U -> data(31, 0),
          1.U -> data(63, 32),
          2.U -> data(95, 64),
          3.U -> data(127, 96),
        ),
      )
    }
  }

  val hitted           = hit.reduce(_ || _)
  val ans_hitted_valid = hitted && ShiftRegister(hitted, 1)

  val idle :: state0 :: state1 :: waiting :: replace :: Nil = Enum(5)

  val success = RegInit(0.U(32.W))
  val all     = RegInit(0.U(32.W))
  dontTouch(success)
  dontTouch(all)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      ans_valid := false.B
      when(cached) {
        when(io.fetch.request.fire || saved_query) {
          req_ready   := false.B
          saved_query := true.B
          saved_addr  := addr
          all         := all + 1.U
          // the former inst is hitted?
          when(hitted) {
            success   := success + 1.U
            ans_valid := true.B
            when(io.fetch.cango) {
              stall     := false.B
              req_ready := true.B
              state     := idle
            }.otherwise {
              saved_inst  := hitdata
              saved_valid := true.B
              state       := waiting
            }
          }.otherwise {
            arvalid := true.B
            ar.addr := qaddr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
            ar.size := 2.U
            ar.len  := (BANK_WIDTH / 4 - 1).U
            rready  := false.B
            state   := replace
          }
        }
      }.otherwise {
        // uncached
        when(io.fetch.request.fire) {
          arvalid := true.B
          ar.addr := io.fetch.request.bits
          rready  := false.B
          state   := state0
        }.elsewhen(saved_query) {
          saved_query := false.B
          arvalid     := true.B
          ar.addr     := saved_addr
          rready      := false.B
          state       := state0
        }
      }
    }
    is(state0) {
      stall     := true.B
      req_ready := false.B
      when(io.axi.ar.ready) {
        arvalid := false.B
        rready  := true.B
        state   := state1
      }
    }
    is(state1) {
      stall     := true.B
      req_ready := false.B
      when(io.axi.r.valid) {
        ans_valid := true.B
        ans_bits  := io.axi.r.bits.data
        when(io.fetch.cango) {
          stall     := false.B
          req_ready := true.B
          state     := idle
        }.otherwise {
          saved_inst  := io.axi.r.bits.data
          saved_valid := true.B
          state       := waiting
        }
      }
    }
    is(replace) {
      req_ready := false.B
      when(io.axi.ar.valid) {
        when(io.axi.ar.ready) {
          arvalid := false.B
          rready  := true.B
        }
      }.elsewhen(io.axi.r.fire) {
        val wmove = Mux1H(
          Seq(
            wmask(3) -> 96.U,
            wmask(2) -> 64.U,
            wmask(1) -> 32.U,
            wmask(0) -> 0.U,
          ),
        )
        wdata := wdata | (io.axi.r.bits.data << wmove)
        wmask := wmask << 1.U
        when(io.axi.r.bits.last) {
          rready := false.B
        }
      }.elsewhen(!io.axi.r.ready) {
        datasram(waychosen(qindex)).wea     := true.B
        datasram(waychosen(qindex)).dina    := wdata
        tagsram(waychosen(qindex)).wea      := true.B
        tagsram(waychosen(qindex)).dina     := qtag
        validreg(qindex)(waychosen(qindex)) := true.B // 有问题
        wdata                               := 0.U
        wmask                               := 1.U
        ans_valid                           := true.B
        state                               := idle
      }
    }
    is(waiting) {
      // if has request at this state, should save
      when(io.fetch.request.valid) {
        saved_query := true.B
        saved_addr  := io.fetch.request.bits
      }
      ans_bits  := saved_inst
      ans_valid := saved_valid
      when(io.fetch.cango) {
        ans_valid := false.B
        req_ready := true.B
        state     := idle
      }
    }
  }

  io.axi.ar.bits         := ar
  io.axi.ar.valid        := arvalid
  io.axi.r.ready         := rready
  io.fetch.answer.valid  := ans_valid && ans_hitted_valid
  io.fetch.answer.bits   := hitdata
  io.fetch.request.ready := req_ready
}
/*
  对于icache
  一条指令在hit后一定能同时收到下一条指令的请求地址
  miss其实可以看做fake_hit，但下一条指令的请求地址相同

  如果一条指令在hit后不能立刻收到下一条指令的请求地址
  那肯定也无法在下一条指令请求时立刻给出答案
  它总得提前告诉cache请求
  这样在它正式请求时可以即刻知道答案
*/