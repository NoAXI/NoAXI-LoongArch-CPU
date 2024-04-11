package stages

import chisel3._
import chisel3.util._

import bundles._
import config.Functions._
import config.Configs._
import config.Instructions._

class IDIO extends Bundle {
    val imm = Input(UInt(DATA_WIDTH_D.W))
    val imm_out = Output(UInt(DATA_WIDTH_D.W))
}

class ID_EXE extends Module {
    val io = IO(new IDIO)

}