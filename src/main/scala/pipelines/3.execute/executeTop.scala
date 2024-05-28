package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.predictConst._
import const.Parameters._
import Funcs.Functions._

class ExecuteTopIO extends StageBundle {
  val br_exc        = Input(new br)
  val predict_check = Output(new brCheck)
  val br            = Output(new br)
  val forward_data  = Output(new ForwardData)
  // val forward_tag  = Input(Bool())
}

class ExecuteTop extends Module {
  val io   = IO(new ExecuteTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)
  FlushWhen(info, io.flush)

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
  to_info := info
  // to_info.ld_tag := io.forward_tag
  to_info.result := result
  FlushWhen(to_info, io.flush)
  io.to.bits := to_info

  val is_br         = info.func_type === FuncType.bru
  val is_jirl       = info.inst === LA32R.JIRL
  val br_tar        = Mux(is_jirl, info.rj, info.pc) + info.imm
  val succeed       = bru.br_en === info.predict.en && br_tar === info.predict.tar && is_br
  val br_tar_failed = Mux(bru.br_en, br_tar, info.pc + 4.U)
  io.predict_check.en      := is_br
  io.predict_check.succeed := succeed
  io.predict_check.real    := bru.br_en
  io.predict_check.index   := info.pc(INDEX_LENGTH + 1, 2)

  io.br.en       := (is_br && !succeed) || io.br_exc.en
  io.br.tar      := Mux(io.br_exc.en, io.br_exc.tar, br_tar_failed)
  io.flush_apply := (is_br && !succeed) && !info.bubble

  Forward(to_info, io.forward_data)
  when(info.isload) {
    io.forward_data.we := false.B // for data_forward that ld inst's alu's result not used
  }
}
/*
使用axi时，数据前递可能会出现以下问题：它获取了后面流水级的前递数据，但是之所以触发了前递，是因为后面流水级的指令就是dec这条指令
解决：前递时标记这是第几条指令

这里的信号延迟处理有没有更好的方法？？
todo
 */
