package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class ArithmeticTopIO extends SingleStageBundle {
  val forward = Flipped(new ForwardInfoIO)
}

class ArithmeticTop(
    hasBru: Boolean,
) extends Module {
  val io = IO(new ArithmeticTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  // alu
  val alu = Module(new ALU).io
  alu.src1        := info.src1
  alu.src2        := info.src2
  alu.func_type   := info.func_type
  alu.op_type     := info.op_type
  res.rdInfo.data := Mux(info.func_type === FuncType.bru, info.pc + 4.U, alu.result)

  // bru
  if (hasBru) {
    val bru = Module(new BRU).io
    bru.rj        := info.rjInfo.data
    bru.rd        := info.rkInfo.data // decode will set rk.data = rd.data
    bru.func_type := info.func_type
    bru.op_type   := info.op_type

    val is_br   = info.func_type === FuncType.bru
    val is_jirl = info.inst === LA32R.JIRL
    val br_tar  = Mux(is_jirl, info.rjInfo.data, info.pc) + info.imm
    val succeed = Mux(
      bru.br_en,
      bru.br_en === info.predict.en && br_tar === info.predict.tar && is_br,
      bru.br_en === info.predict.en && is_br,
    )
    val br_tar_failed = Mux(bru.br_en, br_tar, info.pc + 4.U)

    res.realBr.en  := (is_br && !succeed)
    res.realBr.tar := br_tar_failed
    res.realBrDir  := bru.br_en
  }

  doForward(io.forward, res, valid)

  if (Config.debug_on) {
    dontTouch(info.rjInfo)
    dontTouch(info.rkInfo)
  }
}
