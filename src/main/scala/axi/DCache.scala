package axi

import chisel3._
import chisel3.util._
import config._

class DCache_IO extends Bundle with Parameters {
  // to axi
  val axi = new AXI_IO

  // act with exe
  val exe     = Flipped(DecoupledIO(UInt(32.W)))
  val to_exe  = DecoupledIO(UInt(32.W))
  val request = Input(Bool())
  val ren     = Input(Bool())
  val wen     = Input(Bool())
  val size    = Input(UInt(32.W))
  val wstrb   = Input(UInt(8.W))
  val addr    = Input(UInt(ADDR_WIDTH.W))
  val wdata   = Input(UInt(DATA_WIDTH.W))

  val finish = Input(Bool())

  // to wb: stall
  val stall = Output(Bool())
}

class DCache extends Module with Parameters {
  val io = IO(new DCache_IO)

  val axi    = RegInit(0.U.asTypeOf(new AXI_IO))
  val to_exe = RegInit(0.U.asTypeOf(new DecoupledIO(UInt(DATA_WIDTH.W))))
  val stall  = RegInit(Bool())

  io.axi    <> axi
  io.to_exe <> to_exe
  io.stall  <> stall

  val saved_valid = RegInit(false.B)
  val saved_data  = RegInit(0.U(DATA_WIDTH.W))

  val idle :: waiting :: state0 :: state1 :: state2 :: state3 :: state4 :: state5 :: Nil = Enum(8)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      to_exe.valid := false.B
      when(io.request) {
        when(io.ren) {
          axi.arvalid := true.B
          axi.arready := false.B
          axi.araddr  := io.addr
          axi.arsize  := io.size
          state       := state0
        }.elsewhen(io.wen) {
          axi.awvalid := true.B
          axi.awaddr  := io.addr
          axi.awlen   := 0.U
          axi.awsize  := io.size
          axi.wstrb   := io.wstrb
          axi.wvalid  := true.B
          axi.wdada   := io.wdata
          axi.bready  := false.B
          state       := state2
        }
      }
    }
    is(state0) {
      stall := true.B
      when(io.axi.arready) {
        axi.arvalid := false.B
        axi.rready  := true.B
        state       := state1
      }
    }
    is(state1) {
      stall := true.B
      when(io.axi.rvalid) {
        to_exe.valid := true.B
        to_exe.bits  := axi.rdata

        when(io.finish) {
          state := idle
        }.otherwise {
          saved_data  := to_exe.bits
          saved_valid := to_exe.valid
          state       := waiting
        }
      }
    }
    is(state2) {
      when(io.axi.wready && io.axi.awready) {
        axi.awvalid := false.B
        axi.wvalid  := false.B
        axi.bready  := true.B
        state       := state5
      }.elsewhen(io.axi.wready) {
        axi.awvalid := true.B
        axi.wvalid  := false.B
        axi.bready  := false.B
        state       := state4
      }.elsewhen(io.axi.awready) {
        axi.awvalid := false.B
        axi.wvalid  := true.B
        axi.bready  := false.B
        state       := state3
      }
    }
    is(state3) {
      when(io.axi.wready) {
        axi.awvalid := false.B
        axi.wvalid  := false.B
        axi.bready  := true.B
        state       := state5
      }
    }
    is(state4) {
      when(io.axi.awready) {
        axi.awvalid := false.B
        axi.wvalid  := false.B
        axi.bready  := true.B
        state       := state5
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
      to_exe.bits  := saved_data
      to_exe.valid := saved_valid
      when(io.finish) {
        to_exe.valid := false.B
        state        := idle
      }
    }
  }
}
