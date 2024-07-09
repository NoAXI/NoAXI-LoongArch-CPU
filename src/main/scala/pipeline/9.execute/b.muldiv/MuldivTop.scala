package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class MuldivTopIO extends SingleStageBundle {
  val forward = Flipped(new ForwardInfoIO)
  val awake   = Output(new AwakeInfo)
}

class MuldivTop extends Module {
  val io = IO(new MuldivTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  val is_mul = info.func_type === FuncType.mul
  val is_div = info.func_type === FuncType.div
  val mul    = Module(new Mul).io
  val src1   = info.rjInfo.data
  val src2   = info.rkInfo.data

  mul.running := is_mul
  mul.op_type := info.op_type
  mul.src1    := src1
  mul.src2    := src2

  val div = Module(new Div).io
  div.flush   := io.flush
  div.running := is_div
  div.op_type := info.op_type
  div.src1    := src1
  div.src2    := src2

  busy := (div.running && !div.complete) || (mul.running && !mul.complete)

  // avoid multi-request
  val mul_complete = RegInit(false.B)
  val div_complete = RegInit(false.B)
  if (Config.debug_on) {
    dontTouch(mul_complete)
    dontTouch(div_complete)
  }
  when(is_mul && mul.complete) {
    mul_complete := true.B
  }.elsewhen(is_div && div.complete) {
    div_complete := true.B
  }
  when(mul_complete) {
    mul.running := false.B
    busy        := false.B
  }
  when(div_complete) {
    div.running := false.B
    busy        := false.B
  }
  when(io.from.fire) {
    mul_complete := false.B
    div_complete := false.B
  }

  val result = MateDefault(
    info.func_type,
    0.U,
    List(
      FuncType.div -> div.result,
      FuncType.mul -> mul.result,
    ),
  )

  res.result := result
  doForward(io.forward, res, valid)

  io.awake.valid := valid && info.iswf && io.to.fire
  io.awake.preg  := info.rdInfo.preg
}
