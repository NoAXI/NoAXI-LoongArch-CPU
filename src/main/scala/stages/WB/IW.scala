package stages

import chisel3._
import chisel3.util._

import config._
import config.Functions._

class IW_IO extends Bundle with Parameters {
    val from = Flipped(DecoupledIO(new info))
    val to = DecoupledIO(new info)

    //** to debug_sign
    val debug_wb_pc = Output(UInt(32.W)) 
    val debug_wb_rf_we = Output(UInt(4.W))
    val debug_wb_rf_wnum = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(32.W))

    //** to rf
    val ws_to_rf_bus = Output(UInt(38.W))

    //** to data_sram
    val data_sram_rdata = Input(UInt(INST_WIDTH.W))
}

class IW extends Module with Parameters {
    val io = IO(new IW_IO)

    //与上一流水级握手，获取上一流水级信息
    val ms_to_ws_bus_r = ConnectGetBus(io.from, io.to)

    val ws_gr_we        = ms_to_ws_bus_r.gr_we.asBool
    val ws_dest         = ms_to_ws_bus_r.dest
    val ws_final_result = ms_to_ws_bus_r.alu_result
    val ws_pc           = ms_to_ws_bus_r.pc
    val ms_res_from_mem = ms_to_ws_bus_r.res_from_mem.asBool

    //写寄存器，传给ds
    val rf_we = ws_gr_we && io.to.valid
    val rf_waddr = ws_dest
    val rf_wdata = Mux(ms_res_from_mem, io.data_sram_rdata, ws_final_result)
    io.ws_to_rf_bus := Cat(rf_we,rf_waddr,rf_wdata)

    val to_info = WireDefault(0.U.asTypeOf(new info))
    io.to.bits := to_info

    io.debug_wb_pc := ws_pc
    io.debug_wb_rf_we := Fill(4, rf_we)
    io.debug_wb_rf_wnum := rf_waddr
    io.debug_wb_rf_wdata := rf_wdata
}