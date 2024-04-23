package stages

import chisel3._
import chisel3.util._

import config._
import config.Functions._

object ALUOpType {
    def add     = "b100000".U
    def sub     = "b100001".U
    def slt     = "b100010".U
    def sltu    = "b100011".U
    def and     = "b100100".U
    def nor     = "b100101".U
    def or      = "b100110".U
    def xor     = "b100111".U
    def sll     = "b101000".U
    def srl     = "b101001".U
    def sra     = "b101010".U
    def lui     = "b101011".U
}

class ALU_IO extends Bundle with Parameters {
    val alu_op = Input(UInt(6.W))
    val alu_src1 = Input(UInt(32.W))
    val alu_src2 = Input(UInt(32.W))
    val alu_result = Output(UInt(32.W))
}

class ALU extends Module with Parameters {
    val io = IO(new ALU_IO)
    
    io.alu_result := MateDefault(io.alu_op, 0.U, List(
        ALUOpType.add  ->    (SignedExtend(io.alu_src1 + io.alu_src2, 32)),
        ALUOpType.sub  ->    (SignedExtend(io.alu_src1 - io.alu_src2, 32)),
        ALUOpType.slt  ->    (Mux(io.alu_src1.asSInt < io.alu_src2.asSInt, 1.U, 0.U)),
        ALUOpType.sltu ->    (Mux(io.alu_src1 < io.alu_src2, 1.U, 0.U)),
        ALUOpType.and  ->    (io.alu_src1 & io.alu_src2),
        ALUOpType.nor  ->    (~(io.alu_src1 | io.alu_src2)),
        ALUOpType.or   ->    (io.alu_src1 | io.alu_src2),
        ALUOpType.xor  ->    (io.alu_src1 ^ io.alu_src2),
        ALUOpType.sll  ->    (io.alu_src1 << (io.alu_src2(4, 0))),
        ALUOpType.srl  ->    (io.alu_src1 >> (io.alu_src2(4, 0))),
        ALUOpType.sra  ->    (Cat(Fill(32, io.alu_src1(31)), io.alu_src1) >> (io.alu_src2(4, 0)))(31, 0),
        ALUOpType.lui  ->    (io.alu_src2)
    ))
}
