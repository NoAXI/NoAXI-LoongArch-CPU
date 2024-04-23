package stages

import chisel3._
import chisel3.util._

import config._
import config.Functions._

class IM_IO extends Bundle with Parameters {
    val from = Flipped(DecoupledIO(new info))
    val to = DecoupledIO(new info)

    //** from data-sram
    val data_sram_rdata = Input(UInt(INST_WIDTH.W))

    //** to sram
    val data_sram_en = Output(Bool())
    val data_sram_we = Output(UInt(INST_WIDTH_B.W))
    val data_sram_addr = Output(UInt(ADDR_WIDTH.W)) 
    val data_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IM extends Module with Parameters {
    val io = IO(new IM_IO)

    //与上一流水级握手，获取上一流水级信息
    val info = ConnectGetBus(io.from, io.to)

    // 取出上级流水级缓存内容
    val ms_res_from_mem     = info.res_from_mem.asBool
    val ms_gr_we            = info.gr_we.asBool
    val ms_dest             = info.dest
    val ms_alu_result       = info.alu_result
    val ms_pc               = info.pc
    val mem_we              = info.mem_we
    val rkd_value           = info.rkd_value

    //传递信息
    val to_info = WireDefault(0.U.asTypeOf(new info))
    to_info.gr_we := ms_gr_we
    to_info.dest := ms_dest
    to_info.alu_result := ms_alu_result
    to_info.pc := ms_pc
    to_info.res_from_mem := ms_res_from_mem
    io.to.bits := to_info

    io.data_sram_en := true.B
    io.data_sram_we := Fill(4, mem_we)
    io.data_sram_addr := ms_alu_result
    io.data_sram_wdata := rkd_value
}