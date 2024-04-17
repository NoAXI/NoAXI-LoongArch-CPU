package OtherBus

import chisel3._
import chisel3.util._

import Parameters._

class BranchBus extends Bundle with Parameters {
    val valid = Output(Bool())
    val pc_target = Output(UInt(ADDR_WIDTH.W))
}

class RegFileBus extends Bundle with Parameters {
    val valid = Output(Bool())
    val waddr = Output(UInt(ADDR_WIDTH_REG.W))
    val wdata = Output(UInt(DATA_WIDTH.W))
}