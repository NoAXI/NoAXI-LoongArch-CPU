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
  val stall = Output(Bool())
}

class iCache extends Module {
  val io = IO(new iCacheIO)

  // 2^8 line * 16 B/line = 4KB
  val datasram = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val tagsram  = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first(TAG_WIDTH, LINE_WIDTH)).io)
  val validreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))
  val dirtyreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))

  val qaddr   = RegInit(0.U(ADDR_WIDTH.W))
  val qtag    = RegInit(0.U(TAG_WIDTH.W))
  val qindex  = RegInit(0.U(8.W))
  val qoffset = RegInit(0.U(4.W))

  // val data     = RegInit(VecInit(Seq.fill(WAY_WIDTH)(VecInit(Seq.fill(LINE_SIZE)(0.U(8.W))))))
  // val tag      = RegInit(VecInit(Seq.fill(WAY_WIDTH)(0.U(TAG_WIDTH.W))))
  val valid   = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val dirty   = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val wdata   = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask   = RegInit(1.U(4.W))
  val hit     = WireDefault(VecInit(Seq.fill(WAY_WIDTH)(false.B))).suggestName("hit")
  val hitdata = WireDefault(0.U(DATA_WIDTH.W))
  // val writepos = RegInit(1.U(4.W))
  val lru = RegInit(2.U(2.W)) // random temp

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

  val ram_addr = Mux(saved_query, saved_addr(11, 4), io.fetch.request.bits(11, 4))
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
  ar.id    := DontCare
  // ar.len   := 0.U
  ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  val idle :: state0 :: state1 :: waiting :: checkhit :: replace :: Nil = Enum(6)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      ans_valid := false.B
      when(cached) {
        when(io.fetch.request.fire) {
          val _qindex = io.fetch.request.bits(11, 4)
          qaddr   := io.fetch.request.bits
          qtag    := io.fetch.request.bits(31, 12)
          qindex  := _qindex
          qoffset := io.fetch.request.bits(3, 0)
          for (i <- 0 until WAY_WIDTH) {
            valid(i)          := validreg(_qindex)(i)
            dirty(i)          := dirtyreg(_qindex)(i)
          }
          state := checkhit
        }.elsewhen(saved_query) {
          saved_query := false.B
          val _qindex = saved_addr(11, 4)
          qaddr   := saved_addr
          qtag    := saved_addr(31, 12)
          qindex  := _qindex
          qoffset := saved_addr(3, 0)
          for (i <- 0 until WAY_WIDTH) {
            valid(i)          := validreg(_qindex)(i)
            dirty(i)          := dirtyreg(_qindex)(i)
          }
          state := checkhit
        }
      }.otherwise {
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
    is(checkhit) {
      req_ready := false.B
      for (i <- 0 until WAY_WIDTH) {
        val data = datasram(i).douta
        when(valid(i) && tagsram(i).douta === qtag) {
          hit(i) := true.B
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
          ans_bits  := hitdata
          ans_valid := true.B
        }
      }
      when(hit.asUInt.orR) {
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
        saved_query := true.B
        saved_addr  := qaddr
        arvalid     := true.B
        ar.addr     := qaddr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
        ar.size     := 2.U
        ar.len      := (BANK_WIDTH / 4).U
        rready      := false.B
        state       := replace
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
        when(!io.axi.r.bits.last) {
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
        }.otherwise {
          rready                := false.B
          datasram(lru).wea     := true.B
          datasram(lru).dina    := wdata
          tagsram(lru).wea      := true.B
          tagsram(lru).dina     := qtag
          validreg(qindex)(lru) := true.B
          wdata                 := 0.U
          wmask                 := 1.U
        }
      }.elsewhen(!io.axi.r.ready) {
        state := idle
      }
    }
    is(waiting) {
      // 如果在这个阶段收到请求，需要保存住
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
  io.fetch.answer.valid  := ans_valid
  io.fetch.answer.bits   := ans_bits
  io.fetch.request.ready := req_ready
  io.stall               := stall
}
