package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class ALU_IO extends Bundle with Parameters {
  val alu_op     = Input(AluOpType())
  val alu_src1   = Input(UInt(DATA_WIDTH.W))
  val alu_src2   = Input(UInt(DATA_WIDTH.W))
  val alu_result = Output(UInt(DATA_WIDTH.W))
}

class ALU extends Module with Parameters {
  val io = IO(new ALU_IO)

  io.alu_result := MateDefault(
    io.alu_op,
    0.U,
    List(
      AluOpType.add    -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.readb  -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.readbu -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.readh  -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.readhu -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.readw  -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.writeb -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.writeh -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      MemOpType.writew -> (SignedExtend(io.alu_src1 + io.alu_src2, DATA_WIDTH)),
      AluOpType.sub    -> (SignedExtend(io.alu_src1 - io.alu_src2, DATA_WIDTH)),
      AluOpType.slt    -> (Mux(io.alu_src1.asSInt < io.alu_src2.asSInt, 1.U, 0.U)),
      AluOpType.sltu   -> (Mux(io.alu_src1 < io.alu_src2, 1.U, 0.U)),
      AluOpType.and    -> (io.alu_src1 & io.alu_src2),
      AluOpType.nor    -> (~(io.alu_src1 | io.alu_src2)),
      AluOpType.or     -> (io.alu_src1 | io.alu_src2),
      AluOpType.xor    -> (io.alu_src1 ^ io.alu_src2),
      AluOpType.sll    -> (io.alu_src1 << (io.alu_src2(4, 0))),
      AluOpType.srl    -> (io.alu_src1 >> (io.alu_src2(4, 0))),
      AluOpType.sra    -> (Cat(Fill(32, io.alu_src1(31)), io.alu_src1) >> (io.alu_src2(4, 0)))(DATA_WIDTH - 1, 0),
      AluOpType.lui    -> (io.alu_src2),
    ),
  )
}
