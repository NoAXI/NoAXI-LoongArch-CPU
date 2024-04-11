package stages

import chisel3._
import chisel3.util._

import bundles._
import config._
import config.Functions._

class IDIO extends Bundle with Parameters {
    val imm = Input(UInt(DATA_WIDTH_D.W))
    val imm_out = Output(UInt(DATA_WIDTH_D.W))
}

class ID_EXE extends Module with Parameters {
    val io = IO(new IDIO)

}