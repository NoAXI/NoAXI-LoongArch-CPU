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

  val result = MateDefault(
    io.op_type,
    0.U,
    List(
      AluOpType.add  -> (SignedExtend(add_result, DATA_WIDTH)),
      AluOpType.sub  -> (SignedExtend(io.src1 - io.src2, DATA_WIDTH)),
      AluOpType.slt  -> (Mux(io.src1.asSInt < io.src2.asSInt, 1.U, 0.U)),
      AluOpType.sltu -> (Mux(io.src1 < io.src2, 1.U, 0.U)),
      AluOpType.and  -> (io.src1 & io.src2),
      AluOpType.nor  -> (~(io.src1 | io.src2)),
      AluOpType.or   -> (io.src1 | io.src2),
      AluOpType.xor  -> (io.src1 ^ io.src2),
      AluOpType.sll  -> (io.src1 << (io.src2(4, 0))),
      AluOpType.srl  -> (io.src1 >> (io.src2(4, 0))),
      AluOpType.sra  -> (Cat(Fill(32, io.src1(31)), io.src1) >> (io.src2(4, 0)))(DATA_WIDTH - 1, 0),
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
