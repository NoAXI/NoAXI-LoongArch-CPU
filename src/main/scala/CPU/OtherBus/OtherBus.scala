package OtherBus

import chisel3._
import chisel3.util._

import Parameters._

class BranchBus extends Bundle with Parameters {
    val valid = Bool()
    val pc_target = UInt(ADDR_WIDTH.W)
}

class RegFileBus extends Bundle with Parameters {
    val valid = Bool()
    val waddr = UInt(ADDR_WIDTH_REG.W)
    val wdata = UInt(DATA_WIDTH.W)
}