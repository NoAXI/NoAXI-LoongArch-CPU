package components

import chisel3._
import chisel3.util._

import bundles._
import config._
import config.Functions._

// class GRRegIO extends Bundle {
    // val GRReg = Flipped(new GRReg_ALU_IO())
    // val rd = Input(UInt(DATA_WIDTH_D.W)) // 来自于ALU
    // val rj_addr = Input(UInt(GR_ADDR_WIDTH.W)) // 来自于Decoder
    // val rk_addr = Input(UInt(GR_ADDR_WIDTH.W)) // 来自于Decoder
    // val rd_addr = Input(UInt(GR_ADDR_WIDTH.W)) // 来自于Decoder
    // wb阶段的写入数据
// }

class GRReg extends Module with Parameters {
    val io = IO(new GRRegIO)

    val GRReg = RegInit(VecInit(Seq.fill(GR_LEN)(0.U(DATA_WIDTH_D.W))))

    // io.ALU_GRReg_IO.rj := GRReg(io.rj_addr)
    // io.ALU_GRReg_IO.rk := GRReg(io.rk_addr)
    // io.ALU_GRReg_IO.rd_in := GRReg(io.rd_addr)

    // GRReg(io.rd_addr) := io.ALU_GRReg_IO.rd
}