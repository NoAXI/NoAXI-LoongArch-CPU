package components

import chisel3._
import chisel3.util._

import bundles._
import config.Functions._
import config.Configs._
import config.Instructions._

class ALUIO extends Bundle {
    val instruction = Input(UInt(INST_WIDTH.W))
    val rj = Input(UInt(DATA_WIDTH_D.W))
    val rk = Input(UInt(DATA_WIDTH_D.W))
    val rd_in = Input(UInt(DATA_WIDTH_D.W))
    val imm = Input(UInt(DATA_WIDTH_D.W))

    // val GRReg_ALU_IO = new GRReg_ALU_IO()
    val rd = Output(UInt(DATA_WIDTH_D.W))
}

class ALU extends Module {
    val io = IO(new ALUIO)

    io.rd := 0.U

    when (io.instruction === ADD_W) {
        io.rd := SignedExtend(io.rj(31, 0) + io.rk(31, 0), DATA_WIDTH_D)
    } .elsewhen (io.instruction === ADD_D) {
        io.rd := io.rj + io.rk
    } .elsewhen (io.instruction === SUB_W) {
        io.rd := SignedExtend(io.rj(31, 0) - io.rk(31, 0), DATA_WIDTH_D)
    } .elsewhen (io.instruction === SUB_D) {
        io.rd := io.rj - io.rk
    } .elsewhen (io.instruction === ADDI_W) {
        io.rd := SignedExtend(io.rj(31, 0) + SignedExtend(io.imm(11, 0), DATA_WIDTH_W), 
        DATA_WIDTH_D)
    } .elsewhen (io.instruction === ADDI_D) {
        io.rd := io.rj + SignedExtend(io.imm(11, 0), DATA_WIDTH_D)
    } .elsewhen (io.instruction === ADDU16I_D) {
        io.rd := io.rj + SignedExtend(Cat(io.imm(15, 0), Fill(16, 0.U)), DATA_WIDTH_D)
    } .elsewhen (io.instruction === ALSL_W) {
        io.rd := SignedExtend((io.rj(31, 0) << (io.imm(1, 0) + 1.U)) + io.rk(31, 0), DATA_WIDTH_D)
    } .elsewhen (io.instruction === ALSL_WU) {
        io.rd := UnSignedExtend((io.rj(31, 0) << (io.imm(1, 0) + 1.U)) + io.rk(31, 0), DATA_WIDTH_D)
    } .elsewhen (io.instruction === ALSL_D) {
        io.rd := (io.rj << (io.imm(1, 0) + 1.U)) + io.rk
    } .elsewhen (io.instruction === LU12I_W) {
        io.rd := SignedExtend(Cat(io.imm(19, 0), Fill(12, 0.U)), DATA_WIDTH_D)
    } .elsewhen (io.instruction === LU32I_D) {
        io.rd := Cat(SignedExtend(io.imm(19, 0), DATA_WIDTH_W), io.rd_in(31, 0))
    } .elsewhen (io.instruction === LU52I_D) {
        io.rd := Cat(io.imm(11, 0), io.rj(51, 0))
    }
}