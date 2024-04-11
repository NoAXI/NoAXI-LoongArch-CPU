package stages

import chisel3._
import chisel3.util._

import bundles._
import config._
import config.Functions._

class EXEIO extends Bundle with Parameters {
    val cal_result = Input(UInt(DATA_WIDTH_D.W))
    val cal_result_out = Output(UInt(DATA_WIDTH_D.W))
}

class EXE_MEM extends Module with Parameters {
    val io = IO(new EXEIO)
}