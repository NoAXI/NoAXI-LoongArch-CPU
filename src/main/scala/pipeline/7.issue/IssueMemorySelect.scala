package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class IssueMemorySelectIO extends Bundle {
  val fromIssue  = Flipped(DecoupledIO(new SingleInfo))
  val fromBuffer = Flipped(DecoupledIO(new BufferInfo))
  val to         = DecoupledIO(new SingleInfo)
}

class IssueMemorySelect extends Module {
  val io = IO(new IssueMemorySelectIO)

  val toInfo = WireDefault(0.U.asTypeOf(new SingleInfo))
  toInfo.writeInfo := io.fromBuffer.bits

  io.to.bits          := Mux(io.fromBuffer.fire, toInfo, io.fromIssue.bits)
  io.fromBuffer.ready := io.to.ready
  io.fromIssue.ready  := Mux(io.fromBuffer.fire, false.B, io.to.ready)
  io.to.valid         := io.fromBuffer.valid || io.fromIssue.valid
}
