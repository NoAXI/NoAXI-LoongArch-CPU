package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.Parameters._
import Funcs.Functions._

class ExecuteTopIO extends StageBundle {
  val br_exc       = Input(new br)
  val br           = Output(new br)
  val forward_data = Output(new ForwardData)
  val forward_tag  = Input(Bool())
}

class ExecuteTop extends Module {
  val io   = IO(new ExecuteTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)
  when(io.flush) {
    info        := 0.U.asTypeOf(new info)
    info.bubble := true.B
  }

  val alu = Module(new Alu).io
  alu.func_type := info.func_type
  alu.op_type   := info.op_type
  alu.src1      := info.src1
  alu.src2      := info.src2

  val mul = Module(new Mul).io
  mul.running := info.func_type === FuncType.mul
  mul.op_type := info.op_type
  mul.src1    := info.src1
  mul.src2    := info.src2

  val div = Module(new Div).io
  div.running := info.func_type === FuncType.div
  div.op_type := info.op_type
  div.src1    := info.src1
  div.src2    := info.src2

  val bru = Module(new Bru).io
  bru.func_type := info.func_type
  bru.op_type   := info.op_type
  bru.rj        := info.src1
  bru.rd        := info.rd

  busy := (div.running && !div.complete) || (mul.running && !mul.complete)

  val result = MateDefault(
    info.func_type,
    alu.result,
    List(
      FuncType.csr -> info.csr_value,
      FuncType.div -> div.result,
      FuncType.mul -> mul.result,
    ),
  )

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info        := info
  to_info.ld_tag := io.forward_tag
  to_info.result := result
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  val br_tar     = Mux(info.inst === LA32R.JIRL, info.rj, info.pc) + info.imm
  val br_tar_exc = io.br_exc.tar
  io.br.en := bru.br_en || io.br_exc.en
  // to do: can add a signal to info that indicates the jirl inst
  // also: can not delete the add!!
  io.br.tar      := Mux(io.br_exc.en, br_tar_exc, br_tar)
  io.flush_apply := bru.br_en && io.to.valid && !info.bubble

  Forward(to_info, io.forward_data)
  when(info.isload) {
    io.forward_data.we := false.B // for data_forward that ld inst's alu's result not used
  }
}
