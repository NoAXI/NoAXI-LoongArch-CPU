package stages

import chisel3._
import chisel3.util._

import config._

class IM_IO extends Bundle with Parameters {
    //allow in
    val ws_allowin = Input(Bool())
    val ms_allowin = Output(Bool())

    //from es
    val es_to_ms_valid = Input(Bool()) 
    val es_to_ms_bus = Input(UInt(ES_TO_MS_BUS_WIDTH.W)) 

    //** from data-sram
    val data_sram_rdata = Input(UInt(INST_WIDTH.W))

    //to ws
    val ms_to_ws_valid = Output(Bool()) 
    val ms_to_ws_bus = Output(UInt(MS_TO_WS_BUS_WIDTH.W)) 
}

class IM extends Module with Parameters {
    val io = IO(new IM_IO)

    //与上一流水级握手，获取上一流水级信息
    val es_to_ms_bus_r = RegInit(0.U(ES_TO_MS_BUS_WIDTH.W))
    val ms_valid = RegInit(false.B)
    val ms_ready_go = true.B
    io.ms_allowin := !ms_valid || ms_ready_go && io.ws_allowin
    io.ms_to_ws_valid := ms_valid && ms_ready_go
    when (io.ms_allowin) {
        ms_valid := io.es_to_ms_valid
    }
    when (io.es_to_ms_valid && io.ms_allowin) {
        es_to_ms_bus_r := io.es_to_ms_bus
    }

    // 取出上级流水级缓存内容
    val ms_res_from_mem = es_to_ms_bus_r(70)
    val ms_gr_we        = es_to_ms_bus_r(69)
    val ms_dest         = es_to_ms_bus_r(68, 64)
    val ms_alu_result   = es_to_ms_bus_r(63, 32)
    val ms_pc           = es_to_ms_bus_r(31, 0)

    //传递信息
    val ms_final_result = Mux(ms_res_from_mem, io.data_sram_rdata, ms_alu_result)
    io.ms_to_ws_bus := Cat( ms_gr_we, 
                            ms_dest, 
                            ms_final_result, 
                            ms_pc)
}