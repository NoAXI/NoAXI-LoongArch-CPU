package axi

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._

class dCacheIO extends Bundle {
  val axi   = new dCache_AXI
  val mem   = Flipped(new mem_dCache_IO)
  val stall = Output(Bool())
}

class dCache extends Module {
  val io = IO(new dCacheIO)

  val ar          = RegInit(0.U.asTypeOf(new AR))
  val arvalid     = RegInit(false.B)
  val aw          = RegInit(0.U.asTypeOf(new AW))
  val awvalid     = RegInit(false.B)
  val w           = RegInit(0.U.asTypeOf(new W))
  val wvalid      = RegInit(false.B)
  val bready      = RegInit(false.B)
  val rready      = RegInit(false.B)
  val ans_valid   = RegInit(false.B)
  val ans_bits    = RegInit(0.U(INST_WIDTH.W))
  val req_r_ready = RegInit(true.B)
  val req_w_ready = RegInit(true.B)
  val stall       = RegInit(false.B)

  val idle :: state0 :: state1 :: state2 :: state3 :: state4 :: state5 :: waiting :: Nil = Enum(8)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      ans_valid := false.B
      when(io.mem.request_r.fire) {
        arvalid := true.B
        rready  := false.B
        ar.addr := io.mem.request_r.bits
        state   := state0
      }.elsewhen(io.mem.request_w.fire) {
        awvalid := true.B
        aw.addr := io.mem.request_w.bits.addr

        wvalid := true.B
        w.strb := io.mem.request_w.bits.strb
        w.data := io.mem.request_w.bits.data

        bready := false.B
        state  := state2
      }
    }
    is(state0) {
      stall       := true.B
      req_r_ready := false.B
      when(io.axi.ar.ready) {
        arvalid := false.B
        rready  := true.B
        state   := state1
      }
    }
    is(state1) {
      stall       := true.B
      req_r_ready := false.B
      when(io.axi.r.valid) {
        ans_valid   := true.B
        ans_bits    := io.axi.r.bits.data
        stall       := false.B
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
        state     := idle
      }
    }
  }

  ar.id    := DontCare
  ar.len   := 0.U
  ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  aw.id    := 1.U
  aw.len   := 0.U
  aw.size  := 2.U
  aw.burst := 1.U
  aw.lock  := 0.U
  aw.cache := 0.U
  aw.prot  := 0.U

  w.id   := 1.U
  w.last := true.B

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
  io.stall               := stall
}
