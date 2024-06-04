package axi

import chisel3._
import chisel3.util._

import cache._
import bundles._
import Funcs.Functions._
import const.cacheConst._
import const.Parameters._
import configs.CpuConfig

class dCacheIO extends Bundle {
  val axi = new dCache_AXI
  val exe = Flipped(new exe_dCache_IO)
  val mem = Flipped(new _mem_dCache_IO)
}

class dCache extends Module {
  val io = IO(new dCacheIO)

  val datasram   = VecInit.fill(2)(Module(new xilinx_single_port_ram_read_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val tagsram    = VecInit.fill(2)(Module(new xilinx_single_port_ram_read_first(TAG_WIDTH, LINE_WIDTH)).io)
  val validreg   = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(2)(false.B)))))
  val dirtyreg   = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(2)(false.B)))))
  val prev_valid = WireDefault(VecInit(Seq.fill(2)(false.B)))
  val prev_dirty = WireDefault(VecInit(Seq.fill(2)(false.B)))
  val lru        = RegInit(VecInit(Seq.fill(LINE_WIDTH)(false.B)))

  val ar             = RegInit(0.U.asTypeOf(new AR))
  val arvalid        = RegInit(false.B)
  val rready         = RegInit(false.B)
  val aw             = RegInit(0.U.asTypeOf(new AW))
  val awvalid        = RegInit(false.B)
  val w              = RegInit(0.U.asTypeOf(new W))
  val wvalid         = RegInit(false.B)
  val bready         = true.B
  val ans_valid_imm  = WireDefault(false.B)
  val ans_valid      = RegInit(false.B)
  val ans_bits       = RegInit(0.U(DATA_WIDTH.W))
  val after_replace  = RegInit(false.B)
  val prev_addr      = RegInit(0.U(ADDR_WIDTH.W)) // the most begin: set an illegal address
  val prev_tag       = RegInit(0.U(TAG_WIDTH.W))
  val prev_index     = RegInit(0.U(8.W))
  val prev_offset    = RegInit(0.U(2.W))
  val re             = RegInit(false.B)
  val we             = RegInit(false.B)
  val wdata          = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask          = RegInit(1.U(4.W))
  val is_uncached    = RegInit(false.B)
  val write_back     = WireDefault(false.B)
  val is_replace_put = WireDefault(false.B)

  // if pipeline only send a tick request
  when(io.exe.request.valid) {
    prev_addr := io.exe.request.bits.addr
  }
  val cached = prev_addr(31, 16) =/= 0xbfaf.U
  is_uncached := !cached

  // [ )'s address
  val now_addr  = Mux(io.exe.request.valid, io.exe.request.bits.addr, prev_addr)
  val now_index = now_addr(11, 4)

  // prepare for the next
  for (i <- 0 until 2) {
    datasram(i).clka  := clock
    datasram(i).addra := now_index
    datasram(i).wea   := WireDefault(false.B)
    datasram(i).dina  := 0.U
    tagsram(i).clka   := clock
    tagsram(i).addra  := now_index
    tagsram(i).wea    := WireDefault(false.B)
    tagsram(i).dina   := 0.U
    prev_valid(i)     := validreg(now_index)(i)
    prev_dirty(i)     := dirtyreg(now_index)(i)
  }
  prev_index  := now_index
  prev_tag    := now_addr(31, 12)
  prev_offset := now_addr(3, 2)
  val offset_move = MateDefault(
    prev_offset,
    0.U,
    Seq(
      1.U -> 32.U,
      2.U -> 64.U,
      3.U -> 96.U,
    ),
  )
  val b_enable  = Cat((3 to 0 by -1).map(i => Fill(8, io.mem.request.bits.strb(i))))
  val wmove     = Mux1H((3 to 0 by -1).map(i => wmask(i) -> (i * 32).U))
  val waychosen = lru(prev_index).asUInt

  val prev_hit     = VecInit.tabulate(2)(i => tagsram(i).douta === prev_tag && prev_valid(i))
  val prev_hitted  = prev_hit.reduce(_ || _)
  val prev_hit_way = Mux(prev_hit(0), 0.U, 1.U)
  val prev_hitdata = Mux1H(prev_hit, VecInit.tabulate(2)(i => datasram(i).douta))
  val hitdata = MateDefault(
    prev_offset,
    0.U,
    Seq(
      0.U -> prev_hitdata(31, 0),
      1.U -> prev_hitdata(63, 32),
      2.U -> prev_hitdata(95, 64),
      3.U -> prev_hitdata(127, 96),
    ),
  )
  // val ans_hitted_valid = prev_hitted && ShiftRegister(prev_hitted, 1)

  // statistics
  val success = RegInit(0.U(32.W))
  val all     = RegInit(0.U(32.W))

  val idle :: replace_get :: replace_put :: dirty_write :: uncached_r :: uncached_w :: Nil = Enum(6)

  io.mem.answer_imm := false.B

  val state = RegInit(idle)
  switch(state) {
    is(idle) { // checkhit and receive prev_request
      ans_valid := false.B
      when(io.mem.request.fire || after_replace) {
        when(cached) {
          // the former is hitted?
          when(prev_hitted) {
            lru(prev_index)   := !lru(prev_index)
            after_replace     := false.B
            ans_valid         := true.B
            io.mem.answer_imm := true.B
            // ans_bits          := hitdata
            write_back := io.mem.request.bits.we
          }.otherwise {
            state   := replace_get
            arvalid := true.B
            ar.addr := prev_addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
            ar.size := 2.U
            ar.len  := (BANK_WIDTH / 4 - 1).U
            rready  := false.B
            re      := io.mem.request.bits.re
            we      := io.mem.request.bits.we
          }
        }.otherwise {
          is_uncached := true.B
          when(io.mem.request.bits.re) {
            arvalid := true.B
            rready  := false.B
            ar.addr := io.mem.request.bits.addr
            state   := uncached_r
          }.elsewhen(io.mem.request.bits.we) {
            awvalid := true.B
            aw.addr := io.mem.request.bits.addr

            wvalid := true.B
            w.strb := io.mem.request.bits.strb
            w.data := io.mem.request.bits.data
            w.last := true.B

            state := uncached_w
          }
        }
      }
    }
    is(replace_get) {
      when(io.axi.ar.valid) {
        when(io.axi.ar.ready) {
          arvalid := false.B
          rready  := true.B
        }
      }.elsewhen(io.axi.r.fire) {
        wdata := wdata | (io.axi.r.bits.data << wmove)
        wmask := wmask << 1.U
        when(io.axi.r.bits.last) {
          rready := false.B
          wmask  := 1.U
          state  := replace_put
        }
      }
    }
    is(replace_put) {
      when(!prev_dirty(waychosen)) {
        is_replace_put := true.B
        write_back     := true.B
        state          := idle
        wdata          := 0.U
        wmask          := 1.U
      }.otherwise {
        awvalid := true.B
        aw.addr := prev_addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
        aw.size := 2.U
        aw.len  := (BANK_WIDTH / 4 - 1).U

        wvalid := true.B
        w.data := datasram(waychosen).douta(31, 0)
        w.strb := "b1111".U
        w.last := false.B

        state := dirty_write
      }
    }
    is(dirty_write) {
      when(io.axi.aw.fire) {
        awvalid := false.B
      }
      when(io.axi.w.fire) {
        when(w.last) {
          wvalid := false.B
        }.otherwise {
          wmask := wmask << 1.U
          w.data := Mux1H(
            Seq(
              wmask(0) -> datasram(waychosen).douta(63, 32),
              wmask(1) -> datasram(waychosen).douta(95, 64),
              wmask(2) -> datasram(waychosen).douta(127, 96),
            ),
          )
          w.strb := "b1111".U
          w.last := wmask(2).asBool
        }
      }
      when(io.axi.b.fire) {
        state                           := replace_put
        dirtyreg(prev_index)(waychosen) := false.B
      }
    }
    is(uncached_r) {
      when(io.axi.ar.valid) {
        when(io.axi.ar.ready) {
          arvalid := false.B
          rready  := true.B
        }
      }.elsewhen(io.axi.r.fire) {
        ans_valid := true.B
        ans_bits  := io.axi.r.bits.data
        state     := idle
      }
    }
    is(uncached_w) {
      when(io.axi.w.fire) {
        wvalid := false.B
      }
      when(io.axi.aw.fire) {
        awvalid := false.B
      }
      when(io.axi.b.fire) {
        ans_valid := true.B
        ans_bits  := 0.U // stand for it is no mean
        state     := idle
      }
    }
  }

  when(write_back) {
    val chosen_way = Mux(is_replace_put, waychosen, prev_hit_way)
    datasram(chosen_way).wea := true.B
    datasram(chosen_way).dina := Mux(
      is_replace_put,
      wdata,
      writeMask(b_enable << offset_move, prev_hitdata, io.mem.request.bits.data << offset_move),
    )
    when(!is_replace_put) {
      datasram(chosen_way).addra       := prev_index
      dirtyreg(prev_index)(chosen_way) := true.B
    }.otherwise {
      tagsram(chosen_way).wea := true.B
    }
    tagsram(chosen_way).dina         := prev_tag
    validreg(prev_index)(chosen_way) := true.B
  }

  ar.id := 1.U
  // ar.len   := 0.U
  ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  aw.id := 1.U
  // aw.len   := 0.U
  aw.size  := 2.U
  aw.burst := 1.U
  aw.lock  := 0.U
  aw.cache := 0.U
  aw.prot  := 0.U

  w.id := 1.U
  // w.last := true.B

  io.axi.ar.bits       := ar
  io.axi.ar.valid      := arvalid
  io.axi.aw.bits       := aw
  io.axi.aw.valid      := awvalid
  io.axi.w.bits        := w
  io.axi.w.valid       := wvalid
  io.axi.r.ready       := rready
  io.axi.b.ready       := bready
  io.mem.answer.valid  := ans_valid
  io.mem.answer.bits   := Mux(is_uncached, ans_bits, hitdata)
  io.mem.request.ready := true.B
  io.exe.request.ready := state === idle && !write_back // can be improve

  if (CpuConfig.debug_on) {
    dontTouch(hitdata)
    dontTouch(is_uncached)
    dontTouch(b_enable)
    dontTouch(prev_offset)
    dontTouch(now_addr)
  }
}
