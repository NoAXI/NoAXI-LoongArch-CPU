package axi

import chisel3._
import chisel3.util._

import cache._
import bundles._
import Funcs.Functions._
import const.cacheConst._
import const.Parameters._

class dCacheIO extends Bundle {
  val axi = new dCache_AXI
  val mem = Flipped(new mem_dCache_IO)
}

class dCache extends Module {
  val io = IO(new dCacheIO)

  // 2^8 line * 16 B/line = 4KB
  val datasram = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val tagsram  = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first(TAG_WIDTH, LINE_WIDTH)).io)
  val validreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))
  val dirtyreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))

  val qaddr   = RegInit(0.U(ADDR_WIDTH.W))
  val qmask   = RegInit(0.U(DATA_WIDTH.W))
  val qdata   = RegInit(0.U(DATA_WIDTH.W))
  val qtag    = RegInit(0.U(TAG_WIDTH.W))
  val qindex  = RegInit(0.U(8.W))
  val qoffset = RegInit(0.U(4.W))

  val valid   = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val dirty   = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val wdata   = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask   = RegInit(1.U(4.W))
  val hit     = WireDefault(VecInit(Seq.fill(WAY_WIDTH)(false.B))).suggestName("hit")
  val hitdata = WireDefault(0.U(DATA_WIDTH.W))

  val write_buffer = RegInit(0.U.asTypeOf(new CSRWrite))


  // lru
  val lru = RegInit(VecInit(Seq.fill(3)(true.B)))
  def updatelru(way: UInt): Unit = {
    switch(way) {
      is(0.U) { lru(0) := true.B }
      is(1.U) { lru(0) := false.B }
      is(2.U) { lru(1) := true.B }
      is(3.U) { lru(1) := false.B }
    }
    lru(2) := Mux(way(1), true.B, false.B)
  }
  def indexchosen(): UInt = {
    Mux(
      lru(2),
      Mux(lru(1), 3.U, 2.U),
      Mux(lru(0), 1.U, 0.U),
    )
  }

  val ar            = RegInit(0.U.asTypeOf(new AR))
  val arvalid       = RegInit(false.B)
  val aw            = RegInit(0.U.asTypeOf(new AW))
  val awvalid       = RegInit(false.B)
  val w             = RegInit(0.U.asTypeOf(new W))
  val wvalid        = RegInit(false.B)
  val bready        = RegInit(false.B)
  val rready        = RegInit(false.B)
  val ans_valid     = RegInit(false.B)
  val ans_bits      = RegInit(0.U(INST_WIDTH.W))
  val req_r_ready   = RegInit(true.B)
  val req_w_ready   = RegInit(true.B)
  val saved_query_r = RegInit(false.B)
  val saved_addr_r  = RegInit(0.U(ADDR_WIDTH.W))
  // val saved_query_w = RegInit(false.B)
  // val saved_addr_w  = RegInit(0.U(ADDR_WIDTH.W))
  // val saved_wdata   = RegInit(0.U(DATA_WIDTH.W))
  val is_read     = RegInit(true.B)
  val dirty_write = RegInit(false.B)

  val ram_addr = MuxCase(
    0.U,
    Seq(
      io.mem.request_r.fire -> io.mem.request_r.bits(11, 4),
      io.mem.request_w.fire -> io.mem.request_w.bits.addr(11, 4),
      saved_query_r         -> saved_addr_r(11, 4),
    ),
  )
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
  ar.id := DontCare
  // ar.len   := 0.U
  // ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  aw.id    := 1.U
  aw.len   := 0.U
  // aw.size  := 2.U
  aw.burst := 1.U
  aw.lock  := 0.U
  aw.cache := 0.U
  aw.prot  := 0.U

  w.id   := 1.U
  w.last := true.B

  val idle :: state0 :: state1 :: state2 :: state3 :: state4 :: state5 :: checkhit :: replace :: Nil = Enum(9)

  val wmove = Mux1H(
    Seq(
      wmask(3) -> 96.U,
      wmask(2) -> 64.U,
      wmask(1) -> 32.U,
      wmask(0) -> 0.U,
    ),
  )

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      ans_valid := false.B
      when(cached) {
        when(io.mem.request_r.fire || saved_query_r) {
          val addr    = Mux(io.mem.request_r.fire, io.mem.request_r.bits, saved_addr_r)
          val _qindex = addr(11, 4)
          qaddr   := addr
          qtag    := addr(31, 12)
          qindex  := _qindex
          qoffset := addr(3, 0)
          for (i <- 0 until WAY_WIDTH) {
            valid(i) := validreg(_qindex)(i)
            dirty(i) := dirtyreg(_qindex)(i)
          }
          is_read := true.B
          state   := checkhit
        }.elsewhen(io.mem.request_w.fire) {
          val addr    = io.mem.request_w.bits.addr
          val _qindex = addr(11, 4)
          qaddr   := addr
          qmask   := io.mem.request_w.bits.strb
          qdata   := io.mem.request_w.bits.data
          qtag    := addr(31, 12)
          qindex  := _qindex
          qoffset := addr(3, 0)
          for (i <- 0 until WAY_WIDTH) {
            valid(i) := validreg(_qindex)(i)
            dirty(i) := dirtyreg(_qindex)(i)
          }
          is_read := false.B
          state   := checkhit
        }
      }.otherwise {
        when(io.mem.request_r.fire) {
          arvalid := true.B
          rready  := false.B
          ar.addr := io.mem.request_r.bits
          state   := state0
        }.elsewhen(io.mem.request_w.fire) {
          awvalid := true.B
          aw.size := 2.U
          aw.addr := io.mem.request_w.bits.addr

          wvalid := true.B
          w.strb := io.mem.request_w.bits.strb
          w.data := io.mem.request_w.bits.data

          bready := false.B
          state  := state2
        }
      }
    }
    is(state0) {
      req_r_ready := false.B
      when(io.axi.ar.ready) {
        arvalid := false.B
        rready  := true.B
        state   := state1
      }
    }
    is(state1) {
      req_r_ready := false.B
      when(io.axi.r.valid) {
        ans_valid   := true.B
        ans_bits    := io.axi.r.bits.data
        req_r_ready := true.B
        state       := idle
      }
    }
    is(state2) {
      when(io.axi.w.ready && io.axi.aw.ready) {
        awvalid := false.B
        wvalid  := false.B
        bready  := true.B
        state   := state5
      }.elsewhen(io.axi.w.ready) {
        awvalid := true.B
        wvalid  := false.B
        bready  := false.B
        state   := state4
      }.elsewhen(io.axi.aw.ready) {
        awvalid := false.B
        wvalid  := true.B
        bready  := false.B
        state   := state3
      }
    }
    is(state3) {
      when(io.axi.w.ready) {
        awvalid := false.B
        wvalid  := false.B
        bready  := true.B
        state   := state5
      }
    }
    is(state4) {
      when(io.axi.aw.ready) {
        awvalid := false.B
        wvalid  := false.B
        bready  := true.B
        state   := state5
      }
    }
    is(state5) {
      when(io.axi.b.fire) {
        ans_valid := true.B
        when(dirty_write) {
          dirty_write := false.B
          state       := replace
        }.otherwise {
          state := idle
        }
      }
    }
    is(checkhit) {
      req_r_ready := false.B
      req_w_ready := false.B
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
          when(is_read) {
            ans_bits  := hitdata
            ans_valid := true.B
          }.otherwise {
            dirty(i)         := true.B
            datasram(i).wea  := true.B
            datasram(i).dina := writeMask((qmask << wmove), datasram(i).douta, (qdata << wmove))
          }
        }
      }
      when(hit.asUInt.orR) {
        req_r_ready := true.B
        req_w_ready := true.B
        state       := idle
      }.otherwise {
        saved_query_r := Mux(is_read, true.B, false.B)
        saved_addr_r  := qaddr
        arvalid       := true.B
        ar.addr       := qaddr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
        ar.size       := 4.U
        ar.len        := (BANK_WIDTH / 4).U
        rready        := false.B
        state         := replace
      }
    }
    is(replace) {
      req_r_ready := false.B
      req_w_ready := false.B
      val index = indexchosen()
      when(dirty(index)) {
        // if has write_buffer
        // when(write_buffer.we) {
        //   awvalid := true.B
        //   aw.addr := write_buffer.waddr
        //   wvalid  := true.B
        //   w.strb  := write_buffer.wmask
        //   w.data  := write_buffer.wdata

        //   bready := false.B
        //   state  := state2
        // }
        // write_buffer.we    := true.B
        // write_buffer.wmask := qmask
        // write_buffer.waddr := qaddr
        // write_buffer.wdata := datasram(index).douta
        dirty_write  := true.B
        dirty(index) := false.B
        awvalid      := true.B
        aw.addr      := qaddr
        aw.size      := 4.U
        wvalid       := true.B
        w.strb       := qmask
        w.data       := datasram(index).douta
        bready       := false.B
        state        := state2
      }.otherwise {
        when(io.axi.ar.valid) {
          when(io.axi.ar.ready) {
            arvalid := false.B
            rready  := true.B
          }
        }.elsewhen(io.axi.r.fire) {
          when(!io.axi.r.bits.last) {
            wdata := wdata | (io.axi.r.bits.data << wmove)
            wmask := wmask << 1.U
          }.otherwise {
            rready                  := false.B
            datasram(index).wea     := true.B
            datasram(index).dina    := wdata
            tagsram(index).wea      := true.B
            tagsram(index).dina     := qtag
            validreg(qindex)(index) := true.B
            wdata                   := 0.U
            wmask                   := 1.U
          }
        }.elsewhen(!io.axi.r.ready) {
          state := idle
        }
      }
    }
  }

  io.axi.ar.bits         := ar
  io.axi.ar.valid        := arvalid
  io.axi.aw.bits         := aw
  io.axi.aw.valid        := awvalid
  io.axi.w.bits          := w
  io.axi.w.valid         := wvalid
  io.axi.r.ready         := rready
  io.axi.b.ready         := bready
  io.mem.answer.valid    := ans_valid
  io.mem.answer.bits     := ans_bits
  io.mem.request_r.ready := req_r_ready
  io.mem.request_w.ready := req_w_ready
}
