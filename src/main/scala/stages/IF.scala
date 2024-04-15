package stages

import chisel3._
import chisel3.util._

import config._

class IF_IO extends Bundle with Parameters {
    //allow in
    val ds_allowin = Input(Bool())
    val fs_allowin = Output(Bool())

    //from sram
    val inst_sram_rdata = Input(UInt(INST_WIDTH.W))

    //** from ds
    val br_bus = Input(UInt(BR_BUS_WIDTH.W))

    //to ds
    val fs_to_ds_valid = Output(Bool())
    val fs_to_ds_bus = Output(UInt(FS_TO_DS_BUS_WIDTH.W))

    //** to sram
    val inst_sram_en = Output(Bool())
    val inst_sram_we = Output(UInt(INST_WIDTH_B.W))
    val inst_sram_addr = Output(UInt(ADDR_WIDTH.W)) 
    val inst_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IF extends Module with Parameters {
    val io = IO(new IF_IO)

    val to_fs_valid = RegInit(true.B)

    val fs_valid = RegInit(false.B)
    val fs_ready_go = true.B
    io.fs_allowin := !fs_valid || fs_ready_go && io.ds_allowin
    io.fs_to_ds_valid := fs_valid && fs_ready_go
    io.fs_to_ds_bus := Cat(io.inst_sram_rdata, fs_pc)

    when (io.fs_allowin) {
        fs_valid := to_fs_valid
    }

    val fs_pc = RegInit(0x1bfffffc.U(ADDR_WIDTH.W))
    val seq_pc = fs_pc + 4.U
    val next_pc = Mux(br_taken, br_target, seq_pc)
    val br_taken = io.br_bus(BR_BUS_WIDTH-1)
    val br_target = io.br_bus(BR_BUS_WIDTH-2, 0)

    when (to_fs_valid && io.fs_allowin) {
        fs_pc := next_pc
    }

    io.inst_sram_en := to_fs_valid && io.fs_allowin
    io.inst_sram_we := 0.U
    io.inst_sram_addr := next_pc
    io.inst_sram_wdata := 0.U
}