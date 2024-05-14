package axi

import chisel3._
import chisel3.util._
import config._

class DCache_IO extends Bundle with Parameters {
  // to axi
  val axi = new AXI_DCache_IO

  // act with exe
  val exe     = DecoupledIO(UInt(32.W))
  val request = Input(Bool())
  val finish  = Input(Bool())
  val ren     = Input(Bool())
  val wen     = Input(Bool())
  val size    = Input(UInt(32.W))
  val wstrb   = Input(UInt(8.W))
  val addr    = Input(UInt(ADDR_WIDTH.W))
  val wdata   = Input(UInt(DATA_WIDTH.W))

  // to wb: stall
  val stall = Output(Bool())
}

class DCache extends Module with Parameters {
  val io = IO(new DCache_IO)

  val ar    = RegInit(0.U.asTypeOf(new AR))
  val r     = RegInit(0.U.asTypeOf(new R))
  val aw    = RegInit(0.U.asTypeOf(new AW))
  val w     = RegInit(0.U.asTypeOf(new W))
  val b     = RegInit(0.U.asTypeOf(new B))
  val valid = RegInit(false.B)
  val bits  = RegInit(0.U(DATA_WIDTH.W))
  val stall = RegInit(false.B)

  io.axi.ar    <> ar
  io.axi.r     <> r
  io.axi.aw    <> aw
  io.axi.w     <> w
  io.axi.b     <> b
  io.exe.valid := valid
  io.exe.bits  := bits
  io.stall     <> stall

  val saved_valid = RegInit(false.B)
  val saved_data  = RegInit(0.U(DATA_WIDTH.W))

  val idle :: waiting :: state0 :: state1 :: state2 :: state3 :: state4 :: state5 :: Nil = Enum(8)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      valid := false.B
      when(io.request) {
        when(io.ren) {
          ar.valid := true.B
          r.ready  := false.B
          ar.addr  := io.addr
          ar.size  := io.size
          state    := state0
        }.elsewhen(io.wen) {
          aw.valid := true.B
          aw.addr  := io.addr
          aw.len   := 0.U
          aw.size  := io.size
          w.strb   := io.wstrb
          w.valid  := true.B
          w.data   := io.wdata
          b.ready  := false.B
          state    := state2
        }
      }
    }
    is(state0) {
      stall := true.B
      when(io.axi.arready) {
        ar.valid := false.B
        r.ready  := true.B
        state    := state1
      }
    }
    is(state1) {
      stall := true.B
      when(io.axi.rvalid) {
        valid := true.B
        bits  := io.axi.rdata

        when(io.finish) {
          state := idle
        }.otherwise {
          saved_data  := io.axi.rdata
          saved_valid := true.B
          state       := waiting
        }
      }
    }
    is(state2) {
      when(io.axi.wready && io.axi.awready) {
        aw.valid := false.B
        w.valid  := false.B
        b.ready  := true.B
        state    := state5
      }.elsewhen(io.axi.wready) {
        aw.valid := true.B
        w.valid  := false.B
        b.ready  := false.B
        state    := state4
      }.elsewhen(io.axi.awready) {
        aw.valid := false.B
        w.valid  := true.B
        b.ready  := false.B
        state    := state3
      }
    }
    is(state3) {
      when(io.axi.wready) {
        aw.valid := false.B
        w.valid  := false.B
        b.ready  := true.B
        state    := state5
      }
    }
    is(state4) {
      when(io.axi.awready) {
        aw.valid := false.B
        w.valid  := false.B
        b.ready  := true.B
        state    := state5
      }
    }
    is(state5) {
      when(io.axi.bvalid && io.finish) {
        state := idle
      }.elsewhen(io.axi.bvalid && !io.finish) {
        state := waiting
      }
    }
    is(waiting) {
      bits  := saved_data
      valid := saved_valid
      when(io.finish) {
        valid := false.B
        state := idle
      }
    }
  }
}
