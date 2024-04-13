package bundles

import chisel3._
import chisel3.util._

import config._

class PCRegIO extends Bundle with Parameters {
    val ctrlBranch = Input(Bool())
    val ctrlJump = Input(Bool())
    val addr = Input(UInt(ADDR_WIDTH.W))
    val inst_addr = Output(UInt(ADDR_WIDTH.W))
    val pc = Output(UInt(ADDR_WIDTH.W))
}

class MemInstIO extends Bundle with Parameters {
    val inst_addr = Input(UInt(ADDR_WIDTH.W))
    val enOut = Output(Bool())
    val inst = Output(UInt(INST_WIDTH.W))
}

class IF_ID_IO extends Bundle with Parameters {
    val enIn = Input(Bool())
    val pc = Input(UInt(ADDR_WIDTH.W))
    val instIn = Input(UInt(INST_WIDTH.W))
    val enOut = Output(Bool())
    val instOut = Output(UInt(INST_WIDTH.W))
}

class DecoderIO extends Bundle with Parameters {
    val enIn = Input(Bool())
    val inst = Input(UInt(INST_WIDTH.W))

    val enOut = Output(Bool())
    val instType = Output(UInt(INST_WIDTH.W))

    val rj_addr = Output(UInt(ADDR_WIDTH.W))
    val rk_addr = Output(UInt(ADDR_WIDTH.W))
    val rd_addr = Output(UInt(ADDR_WIDTH.W))

    val imm = Output(UInt(DATA_WIDTH_D.W))

    val src = Output(Bool())
    val ls = Output(Bool())
    val lsType = Output(UInt(LS_TYPE_WIDTH.W))

    val ctrlJump = Output(Bool())
    val jumpAddr = Output(UInt(ADDR_WIDTH.W))
}

class ID_EXE_IO extends Bundle with Parameters {
    val enIn = Input(Bool())
    val instTypeIn = Input(UInt(INST_WIDTH.W))
    val rjIn = Input(UInt(DATA_WIDTH_D.W))
    val rkIn = Input(UInt(DATA_WIDTH_D.W))
    val rdIn = Input(UInt(DATA_WIDTH_D.W))
    val immIn = Input(UInt(DATA_WIDTH_D.W))
    val srcIn = Input(Bool())
    val lsIn = Input(Bool())
    val lsTypeIn = Input(UInt(LS_TYPE_WIDTH.W))

    val enOut_ALU = Output(Bool())
    val instTypeOut = Output(UInt(INST_WIDTH.W))
    val immOut = Output(UInt(DATA_WIDTH_D.W))
    val srcOut = Output(Bool())
    val rjOut = Output(UInt(DATA_WIDTH_D.W))
    val rkOut = Output(UInt(DATA_WIDTH_D.W))
    val rdOut = Output(UInt(DATA_WIDTH_D.W))

    val lsOut = Output(Bool())
    val lsTypeOut = Output(UInt(LS_TYPE_WIDTH.W))
    val addr_rd_Out = Output(UInt(ADDR_WIDTH.W)) 
}

class ALUIO extends Bundle with Parameters {
    val enIn = Input(Bool())
    val instType = Input(UInt(INST_WIDTH.W))
    val imm = Input(UInt(DATA_WIDTH_D.W))
    val src = Input(Bool())
    val rj = Input(UInt(DATA_WIDTH_D.W))
    val rk = Input(UInt(DATA_WIDTH_D.W))
    val rd = Input(UInt(DATA_WIDTH_D.W))

    val enOut = Output(Bool())
    val result = Output(UInt(DATA_WIDTH_D.W))
}

class GRRegIO extends Bundle with Parameters {
    val en = Input(Bool())
    val rj_addr = Input(UInt(ADDR_WIDTH.W))
    val rk_addr = Input(UInt(ADDR_WIDTH.W))
    val rd_addr = Input(UInt(ADDR_WIDTH.W))

    val addr = Input(UInt(ADDR_WIDTH.W))
    val data = Input(UInt(DATA_WIDTH_D.W))

    val rj = Output(UInt(DATA_WIDTH_D.W))
    val rk = Output(UInt(DATA_WIDTH_D.W))
    val rd = Output(UInt(DATA_WIDTH_D.W))

    val dataStore = Output(UInt(DATA_WIDTH_D.W))
}

class EXE_MEM_IO extends Bundle with Parameters {
    val enIn = Input(Bool())
    val resultIn = Input(UInt(DATA_WIDTH_D.W))
    val dataStoreIn = Input(UInt(DATA_WIDTH_D.W))

    val lsIn = Input(Bool())
    val addrIn = Input(UInt(ADDR_WIDTH.W))
    val lsTypeIn = Input(UInt(LS_TYPE_WIDTH.W))

    val enOut = Output(Bool())
    val addrOut = Output(UInt(ADDR_WIDTH.W))
    val lsOut = Output(Bool())
    val lsTypeOut = Output(UInt(LS_TYPE_WIDTH.W))
    val dataStoreOut = Output(UInt(DATA_WIDTH_D.W))

    val resultOut = Output(UInt(DATA_WIDTH_D.W))

    val addr_rd_Out = Output(UInt(ADDR_WIDTH.W))
}

class MemDataIO extends Bundle with Parameters {
    val enIn = Input(Bool())
    val addr = Input(UInt(ADDR_WIDTH.W))
    val ls = Input(Bool())
    val lstype = Input(UInt(LS_TYPE_WIDTH.W))
    val dataStore = Input(UInt(DATA_WIDTH_D.W))

    val enOut = Output(Bool())
    val data = Output(UInt(DATA_WIDTH_D.W))
}

class MEM_WB_IO extends Bundle with Parameters {
    val enIn = Input(Bool())
    val addr_rd_In = Input(UInt(ADDR_WIDTH.W))
    val dataIn = Input(UInt(DATA_WIDTH_D.W))

    val enOut = Output(Bool())
    val addr_rd_Out = Output(UInt(ADDR_WIDTH.W))
    val dataOut = Output(UInt(DATA_WIDTH_D.W))
}

class WriteBackIO extends Bundle with Parameters {
    val en = Input(Bool())
    val addr_rd_In = Input(UInt(ADDR_WIDTH.W))
    val dataIn = Input(UInt(DATA_WIDTH_D.W))

    val addr_rd_Out = Output(UInt(ADDR_WIDTH.W))
    val dataOut = Output(UInt(DATA_WIDTH_D.W))
}