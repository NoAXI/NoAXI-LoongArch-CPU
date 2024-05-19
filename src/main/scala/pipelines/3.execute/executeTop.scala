package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.Parameters._
import Funcs.Functions._

class ExecuteTopIO extends StageBundle {
  val br           = Output(new br)
  val forward_data = Output(new ForwardData)
}

class ExecuteTop extends Module {
  val io   = IO(new ExecuteTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)

  val alu = Module(new Alu).io
  alu.func_type := info.func_type
  alu.op_type   := info.op_type
  alu.src1      := info.src1
  alu.src2      := info.src2

  val bru = Module(new Bru).io
  bru.func_type := info.func_type
  bru.op_type   := info.op_type
  bru.rj        := info.src1
  bru.rd        := info.rd

  val result = alu.result

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info        := info
  to_info.result := result
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.br.en := bru.br_en

  // to do: can add a signal to info that indicates the jirl inst
  // also: can not delete the add!!
  io.br.tar      := Mux(info.inst === LA32R.JIRL, info.rj, info.pc) + info.imm
  io.flush_apply := bru.br_en

  Forward(to_info, io.forward_data)
  when (info.isload) {
    io.forward_data.we := false.B
  }
}