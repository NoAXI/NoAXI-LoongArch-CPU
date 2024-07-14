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

  if (Config.hasBlackBox) {
    when(info.func_type === FuncType.mul) {
      res.rdInfo.data := io.mul.result
    }
  } else {
    // only used in simulator
    val signed_result   = (info.rjInfo.data.asSInt * info.rjInfo.data.asSInt).asUInt
    val unsigned_result = info.rjInfo.data * info.rjInfo.data
    val result = MateDefault(
      info.op_type,
      0.U,
      List(
        MulOpType.shigh -> signed_result(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.slow  -> signed_result(DATA_WIDTH, 0),
        MulOpType.uhigh -> unsigned_result(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.ulow  -> unsigned_result(DATA_WIDTH - 1, 0),
      ),
    )
    when(info.func_type === FuncType.mul) {
      res.rdInfo.data := result
    }
  }

  doForward(io.forward, res, valid)
}
