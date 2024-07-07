package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class ALUIO extends Bundle {
  val info   = Input(new SingleInfo)
  val result = Output(UInt(DATA_WIDTH.W))
}

class ALU extends Module {
  val io = IO(new ALUIO)

  val src1 = io.info.rjInfo.data
  val src2 = io.info.rkInfo.data

  val addResult = src1 + src2
  val subResult = src1 - src2

  val sltResult  = Mux(src1.asSInt < src2.asSInt, 1.U, 0.U)
  val sltuResult = Mux(src1 < src2, 1.U, 0.U)

  val andResult = src1 & src2
  val orResult  = src1 | src2
  val xorResult = src1 ^ src2

  val sllResult = src1 << (src2(4, 0))
  val srlResult = src1 >> (src2(4, 0))
  val sraResult = (src1.asSInt >> (src2(4, 0))).asUInt

  val opType = io.info.op_type
  val result = MateDefault(
    opType,
    0.U,
    List(
      AluOpType.add  -> (addResult),
      AluOpType.sub  -> (subResult),
      AluOpType.slt  -> (sltResult),
      AluOpType.sltu -> (sltuResult),
      AluOpType.and  -> (andResult),
      AluOpType.nor  -> (~orResult),
      AluOpType.or   -> (orResult),
      AluOpType.xor  -> (xorResult),
      AluOpType.sll  -> (sllResult),
      AluOpType.srl  -> (srlResult),
      AluOpType.sra  -> (sraResult),
    ),
  )

  val funcType = io.info.func_type
  when(funcType === FuncType.alu || funcType === FuncType.alu_imm) {
    io.result := result
  }.elsewhen(funcType === FuncType.bru) {
    io.result := addResult
  }.elsewhen(funcType === FuncType.mem) {
    io.result := addResult
  }.otherwise {
    io.result := 0.U
  }
}
