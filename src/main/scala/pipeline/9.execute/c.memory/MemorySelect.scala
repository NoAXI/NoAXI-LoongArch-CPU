package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class MemorySelectIO extends Bundle {
  val fromMem0   = Flipped(DecoupledIO(new SingleInfo))
  val fromBuffer = Flipped(DecoupledIO(new BufferInfo))
  val to         = DecoupledIO(new SingleInfo)
}

class MemorySelect extends Module {
  val io = IO(new MemorySelectIO)

  val toInfo = WireDefault(0.U.asTypeOf(new SingleInfo))
  toInfo.writeInfo   := io.fromBuffer.bits
  toInfo.actualStore := true.B

  io.to.bits          := Mux(io.fromBuffer.fire, toInfo, io.fromMem0.bits)
  io.fromBuffer.ready := io.to.ready
  io.fromMem0.ready   := Mux(io.fromBuffer.fire, false.B, io.to.ready)
  io.to.valid         := io.fromBuffer.valid || io.fromMem0.valid
}
