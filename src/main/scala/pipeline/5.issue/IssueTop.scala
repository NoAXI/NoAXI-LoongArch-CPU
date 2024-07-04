package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class IssueTopIO extends StageBundle {}
class IssueTop extends Module {
  val io = IO(new IssueTopIO)

  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val raw  = stageConnect(io.from, io.to, busy)

  val info  = raw._1
  val valid = io.to.fire && raw._2
  val res   = WireDefault(info)
  flushWhen(raw._1, io.flush)
}
