package stages

import chisel3._
import chisel3.util._

import bundles._
import config.Functions._
import config.Configs._
import config.Instructions._

class EXEIO extends Bundle {
    val cal_result = Input(UInt(DATA_WIDTH_D.W))
    val cal_result_out = Output(UInt(DATA_WIDTH_D.W))
}

class EXE_MEM extends Module {
    val io = IO(new EXEIO)
}