package HandShake

import chisel3._
import chisel3.util._

import CPU._
import Parameters._
import OtherBus._

class Bus extends Bundle with Parameters {
    //if stage
    val pc = UInt(ADDR_WIDTH.W)
    val inst = UInt(INST_WIDTH.W)

    //id stage
    val func_type = FuncType()
    val op_type = UInt(10.W)
    val src1 = UInt(DATA_WIDTH.W)
    val src2 = UInt(DATA_WIDTH.W)
    val imm = UInt(DATA_WIDTH.W)
    val rf_bus = new RegFileBus
    
    //ie/im stage
    val result = UInt(DATA_WIDTH.W)
}

class HandShakeBf extends Bundle with Parameters {
    val ready_in = Output(Bool())
    val valid_out = Input(Bool())
    val bus_out = Input(new Bus)
}

class HandShakeAf extends Bundle with Parameters {
    val ready_in = Input(Bool())
    val valid_out = Output(Bool())
    val bus_out = Output(new Bus)
}