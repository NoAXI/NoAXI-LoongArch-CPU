package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Muldiv0TopIO extends SingleStageBundle {
  val mul = Flipped(new Mul2Mul0IO)
}

class Muldiv0Top extends Module {
  val io = IO(new Muldiv0TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  val mul = Module(new Mul).io
  val div = Module(new Div).io

  val src1   = info.rjInfo.data
  val src2   = info.rkInfo.data
  val is_div = info.func_type === FuncType.div

  // mul
  mul.op_type   := info.op_type // for muldiv2 to get data
  mul.src1      := src1
  mul.src2      := src2
  io.mul.result := mul.result
  mul.op_type2  := io.mul.op_type

  // div
  div.running := is_div
  div.op_type := info.op_type
  div.src1    := src1
  div.src2    := src2

  busy := div.running && !div.complete

  // avoid multi-request
  val div_complete = RegInit(false.B)
  if (Config.debug_on) {
    dontTouch(div_complete)
  }
  when(is_div && div.complete) {
    div_complete := true.B
  }
  when(div_complete) {
    div.running := false.B
    busy        := false.B
  }
  when(io.from.fire) {
    div_complete := false.B
  }

  when(info.func_type === FuncType.div) {
    res.rdInfo.data := div.result
  }
}
