package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class ArithmeticTopIO extends SingleStageBundle {
  val forward = Flipped(new ForwardInfoIO)
}

class ArithmeticTop extends Module {
  val io = IO(new ArithmeticTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  // arith -> forward -> readreg
  doForward(io.forward, res, valid)
}
