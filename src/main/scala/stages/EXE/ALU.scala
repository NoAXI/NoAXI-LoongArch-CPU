package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class ALU_IO extends Bundle with Parameters {
  val alu_op     = Input(UInt(6.W))
  val alu_src1   = Input(UInt(32.W))
  val alu_src2   = Input(UInt(32.W))
  val alu_result = Output(UInt(32.W))
}

class ALU extends Module with Parameters {
  val io = IO(new ALU_IO)

  io.alu_result := MateDefault(
    io.alu_op,
    0.U,
    List(
      AluOpType.add   -> (SignedExtend(io.alu_src1 + io.alu_src2, 32)),
      MemOpType.read  -> (SignedExtend(io.alu_src1 + io.alu_src2, 32)),
      MemOpType.write -> (SignedExtend(io.alu_src1 + io.alu_src2, 32)),
      AluOpType.sub   -> (SignedExtend(io.alu_src1 - io.alu_src2, 32)),
      AluOpType.slt   -> (Mux(io.alu_src1.asSInt < io.alu_src2.asSInt, 1.U, 0.U)),
      AluOpType.sltu  -> (Mux(io.alu_src1 < io.alu_src2, 1.U, 0.U)),
      AluOpType.and   -> (io.alu_src1 & io.alu_src2),
      AluOpType.nor   -> (~(io.alu_src1 | io.alu_src2)),
      AluOpType.or    -> (io.alu_src1 | io.alu_src2),
      AluOpType.xor   -> (io.alu_src1 ^ io.alu_src2),
      AluOpType.sll   -> (io.alu_src1 << (io.alu_src2(4, 0))),
      AluOpType.srl   -> (io.alu_src1 >> (io.alu_src2(4, 0))),
      AluOpType.sra   -> (Cat(Fill(32, io.alu_src1(31)), io.alu_src1) >> (io.alu_src2(4, 0)))(31, 0),
      AluOpType.lui   -> (io.alu_src2),
    ),
  )
}
