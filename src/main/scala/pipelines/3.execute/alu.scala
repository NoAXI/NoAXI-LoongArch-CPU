package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.Parameters._
import Funcs.Functions._

class Alu_IO extends Bundle {
  val func_type = Input(FuncType())
  val op_type   = Input(AluOpType())
  val src1      = Input(UInt(DATA_WIDTH.W))
  val src2      = Input(UInt(DATA_WIDTH.W))
  val result    = Output(UInt(DATA_WIDTH.W))
}

class Alu extends Module {
  val io = IO(new Alu_IO)

  val add_result = io.src1 + io.src2
  val sub_result = io.src1 - io.src2

  val slt_result  = Mux(io.src1.asSInt < io.src2.asSInt, 1.U, 0.U)
  val sltu_result = Mux(io.src1 < io.src2, 1.U, 0.U)

  val and_result = io.src1 & io.src2
  val or_result  = io.src1 | io.src2
  val xor_result = io.src1 ^ io.src2

  val sll_result = io.src1 << (io.src2(4, 0))
  val srl_result = io.src1 >> (io.src2(4, 0))
  val sra_result = (io.src1.asSInt >> (io.src2(4, 0))).asUInt

  val result = MateDefault(
    io.op_type,
    0.U,
    List(
      AluOpType.add  -> (add_result),
      AluOpType.sub  -> (sub_result),
      AluOpType.slt  -> (slt_result),
      AluOpType.sltu -> (sltu_result),
      AluOpType.and  -> (and_result),
      AluOpType.nor  -> (~or_result),
      AluOpType.or   -> (or_result),
      AluOpType.xor  -> (xor_result),
      AluOpType.sll  -> (sll_result),
      AluOpType.srl  -> (srl_result),
      AluOpType.sra  -> (sra_result),
    ),
  )

  when(io.func_type === FuncType.alu || io.func_type === FuncType.alu_imm) {
    io.result := result
  }.elsewhen(io.func_type === FuncType.bru) {
    io.result := add_result
  }.elsewhen(io.func_type === FuncType.mem) {
    io.result := add_result
  }.otherwise {
    io.result := 0.U
  }
}
