package stages

import chisel3._
import chisel3.util._

import config._

class IW_IO extends Bundle with Parameters {
    //allow in
    val ws_allowin = Output(Bool())

    //from es
    val ms_to_ws_valid = Input(Bool()) 
    val ms_to_ws_bus = Input(UInt(MS_TO_WS_BUS_WIDTH.W)) 

    //** to debug_sign
    val debug_wb_pc = Output(UInt(32.W)) 
    val debug_wb_rf_we = Output(UInt(4.W))
    val debug_wb_rf_wnum = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(32.W))

    //** to rf
    val ws_to_rf_bus = Output(UInt(WS_TO_RF_BUS_WIDTH.W))
}

class IW extends Module with Parameters {
    val io = IO(new IW_IO)


    // 取出上级流水级缓存内容
    val ms_to_ws_bus_r = RegInit(0.U(MS_TO_WS_BUS_WIDTH.W))
    val
    (   ws_gr_we,
        ws_dest,
        ws_final_result,
        ws_pc,
    ) =
    (   ms_to_ws_bus_r(69),
        ms_to_ws_bus_r(68, 64),
        ms_to_ws_bus_r(63, 32),
        ms_to_ws_bus_r(31, 0)
    )

    //下一级流水及缓存内容,wb_stage的下一级可以视作rf
    val rf_we = ws_gr_we && ws_valid
    val rf_waddr = ws_dest
    val rf_wdata = ws_final_result
    io.ws_to_rf_bus := Cat(rf_we,rf_waddr,rf_wdata)

    val ws_valid = RegInit(false.B)
    val ws_ready_go = true.B
    io.ws_allowin := !ws_valid || ws_ready_go

    when (io.ws_allowin) {
        ws_valid := io.ms_to_ws_valid
    }

    when (io.ms_to_ws_valid && io.ws_allowin) {
        ms_to_ws_bus_r := io.ms_to_ws_bus
    }


    io.debug_wb_pc := ws_pc
    io.debug_wb_rf_we := Fill(4, rf_we)
    io.debug_wb_rf_wnum := rf_waddr
    io.debug_wb_rf_wdata := rf_wdata
}