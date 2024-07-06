package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

// this pipeline contains of following inst
// memory access inst
// branch check inst
class MemoryTopIO extends SingleStageBundle {}
class MemoryTop extends Module {
  val io = IO(new MemoryTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res
}
