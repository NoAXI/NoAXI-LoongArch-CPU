package axi

import chisel3._
import chisel3.util._
import config._

class ICache_IO extends Bundle with Parameters {
  // to axi
  val axi = new AXI_ICache_IO

  // act with fetch
  val fetch   = DecoupledIO(UInt(32.W))
  val request = Input(Bool())
  val finish  = Input(Bool())
  val addr    = Input(UInt(ADDR_WIDTH.W))

  // to wb: stall all
  val stall = Output(Bool())
}

class ICache extends Module with Parameters {
  val io = IO(new ICache_IO)

  val ar    = RegInit(0.U.asTypeOf(new AR))
  val r     = RegInit(0.U.asTypeOf(new R))
  val valid = RegInit(false.B)
  val bits  = RegInit(0.U(INST_WIDTH.W))
  val stall = RegInit(false.B)

  io.axi.ar      <> ar
  io.axi.r       <> r
  io.fetch.valid := valid
  io.fetch.bits  := bits
  io.stall       <> stall

  val saved_valid = RegInit(false.B)
  val saved_inst  = RegInit(0.U(INST_WIDTH.W))

  val idle :: state0 :: state1 :: waiting :: Nil = Enum(4)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      valid := false.B
      when(io.request) {
        ar.valid := true.B
        ar.addr  := io.addr
        r.ready  := false.B
        ar.len   := 0.U
        ar.size  := 2.U
        state    := state0
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
          stall := false.B
          state := idle
        }.otherwise {
          saved_inst  := io.axi.rdata
          saved_valid := true.B
          state       := waiting
        }
      }
    }
    is(waiting) {
      bits  := saved_inst
      valid := saved_valid
      when(io.finish) {
        valid := false.B
        state := idle
      }
    }
  }
}
