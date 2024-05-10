package axi

import chisel3._
import chisel3.util._
import config._

class ICache_IO extends Bundle with Parameters {
  // to axi
  val axi = new AXI_IO

  // act with fetch
  val fetch    = Flipped(DecoupledIO(UInt(32.W)))
  val to_fetch = DecoupledIO(UInt(32.W))
  val request  = Input(Bool())
  val addr     = Input(UInt(ADDR_WIDTH.W))

  val finish = Input(Bool())

  // to wb: stall all
  val stall = Output(Bool())

  // from ..: is stall?
//   val is_stall = Input(Bool())
}

class ICache extends Module with Parameters {
  val io = IO(new ICache_IO)

  val axi      = RegInit(0.U.asTypeOf(new AXI_IO))
  val to_fetch = RegInit(0.U.asTypeOf(new DecoupledIO(UInt(INST_WIDTH.W))))
  val stall    = RegInit(Bool())

  io.axi      <> axi
  io.to_fetch <> to_fetch
  io.stall    <> stall

  val saved_valid = RegInit(false.B)
  val saved_inst  = RegInit(0.U(INST_WIDTH.W))

  val idle :: state0 :: state1 :: waiting :: Nil = Enum(4)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      to_fetch.valid := false.B
      when(io.request) {
        axi.arvalid := true.B
        axi.arready := false.B
        axi.araddr  := io.addr
        axi.arlen   := 0.U
        axi.arsize  := 4.U
        state       := state0
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
        to_fetch.valid := true.B
        to_fetch.bits  := axi.rdata

        when(io.finish) {
          state := idle
        }.otherwise {
          saved_inst  := to_fetch.bits
          saved_valid := to_fetch.valid
          state       := waiting
        }
      }
    }
    is(waiting) {
      to_fetch.bits  := saved_inst
      to_fetch.valid := saved_valid
      when(io.finish) {
        to_fetch.valid := false.B
        state := idle
      }
    }
  }
}
