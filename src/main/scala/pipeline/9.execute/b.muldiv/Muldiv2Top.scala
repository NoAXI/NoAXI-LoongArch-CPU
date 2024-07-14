package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Mul2Mul0IO extends Bundle {
  val result  = Input(UInt(DATA_WIDTH.W))
  val op_type = Output(MulOpType())
}
class Muldiv2TopIO extends SingleStageBundle {
  val mul     = new Mul2Mul0IO
  val forward = Flipped(new ForwardInfoIO)
}

class Muldiv2Top extends Module {
  val io = IO(new Muldiv2TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  io.mul.op_type := info.op_type

  when(info.func_type === FuncType.mul) {
    res.rdInfo.data := io.mul.result
  }

  doForward(io.forward, res, valid)
}
