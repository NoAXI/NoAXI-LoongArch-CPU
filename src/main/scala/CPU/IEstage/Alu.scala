package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class Alu_IO extends Bundle with Parameters {
    val alu_op = Input(AluOpType())
    val alu_src1 = Input(UInt(DATA_WIDTH.W))
    val alu_src2 = Input(UInt(DATA_WIDTH.W))
    val alu_result = Output(UInt(DATA_WIDTH.W))
}

class Alu extends Module with Parameters {
    val io = IO(new Alu_IO)
    
    io.alu_result := MateDefault(io.alu_op, 0.U, List(
        AluOpType.add  ->    (io.alu_src1 + io.alu_src2),
        AluOpType.sub  ->    (io.alu_src1 - io.alu_src2),
        AluOpType.slt  ->    (Mux(io.alu_src1.asSInt < io.alu_src2.asSInt, 1.U, 0.U)),
        AluOpType.sltu ->    (Mux(io.alu_src1 < io.alu_src2, 1.U, 0.U)),
        AluOpType.and  ->    (io.alu_src1 & io.alu_src2),
        AluOpType.nor  ->    (~(io.alu_src1 | io.alu_src2)),
        AluOpType.or   ->    (io.alu_src1 | io.alu_src2),
        AluOpType.xor  ->    (io.alu_src1 ^ io.alu_src2),
        AluOpType.sll  ->    (SignedExtend(io.alu_src1 << io.alu_src2(4, 0), DATA_WIDTH))(31, 0),
        AluOpType.srl  ->    (SignedExtend(io.alu_src1 >> io.alu_src2(4, 0), DATA_WIDTH))(31, 0),
        AluOpType.sra  ->    (Cat(Fill(32, io.alu_src1(31)), io.alu_src1) >> (io.alu_src2(4, 0)))(31, 0),
        AluOpType.lui  ->    (SignedExtend(Cat(io.alu_src2, 0.U(12.W)), DATA_WIDTH))
    ))
}
