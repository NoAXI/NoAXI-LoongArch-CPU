package stages

import chisel3._
import chisel3.util._

import config._

class Top_IO extends Bundle with Parameters {
    // inst sram interface
    val inst_sram_en = Output(Bool())
    val inst_sram_we = Output(UInt(4.W))
    val inst_sram_addr = Output(UInt(32.W))
    val inst_sram_wdata = Output(UInt(32.W))
    val inst_sram_rdata = Input(UInt(32.W))

    // data sram interface
    val data_sram_en = Output(Bool())
    val data_sram_we = Output(UInt(4.W))
    val data_sram_addr = Output(UInt(32.W))
    val data_sram_wdata = Output(UInt(32.W))
    val data_sram_rdata = Input(UInt(32.W))

    // trace debug interface
    val debug_wb_pc = Output(UInt(32.W))
    val debug_wb_rf_we = Output(UInt(4.W))
    val debug_wb_rf_wnum = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(32.W))
}

class Top extends Module with Parameters {
    val io = IO(new Top_IO)

    val fs = Module(new IF)
    val ds = Module(new ID)
    val es = Module(new IE)
    val ms = Module(new IM)
    val ws = Module(new IW)

    fs.io.ds_allowin <> ds.io.ds_allowin
    fs.io.inst_sram_rdata := io.inst_sram_rdata//
    // fs.io.br_bus <> ds.io.br_bus
    fs.io.br_taken <> ds.io.br_taken
    fs.io.br_target <> ds.io.br_target

    ds.io.es_allowin <> es.io.es_allowin
    ds.io.fs_to_ds_valid <> fs.io.fs_to_ds_valid
    ds.io.fs_to_ds_bus <> fs.io.fs_to_ds_bus
    ds.io.ws_to_rf_bus <> ws.io.ws_to_rf_bus

    es.io.ms_allowin <> ms.io.ms_allowin
    es.io.ds_to_es_valid <> ds.io.ds_to_es_valid
    es.io.ds_to_es_bus <> ds.io.ds_to_es_bus

    ms.io.ws_allowin <> ws.io.ws_allowin
    ms.io.es_to_ms_valid <> es.io.es_to_ms_valid
    ms.io.es_to_ms_bus <> es.io.es_to_ms_bus
    ms.io.data_sram_rdata := io.data_sram_rdata//

    ws.io.ms_to_ws_valid <> ms.io.ms_to_ws_valid
    ws.io.ms_to_ws_bus <> ms.io.ms_to_ws_bus

    ws.io.data_sram_rdata := io.data_sram_rdata

    io.inst_sram_en <> fs.io.inst_sram_en
    io.inst_sram_we <> fs.io.inst_sram_we
    io.inst_sram_addr <> fs.io.inst_sram_addr
    io.inst_sram_wdata <> fs.io.inst_sram_wdata

    // io.data_sram_en <> es.io.data_sram_en
    // io.data_sram_we <> es.io.data_sram_we
    // io.data_sram_addr <> es.io.data_sram_waddr
    // io.data_sram_wdata <> es.io.data_sram_wdata

    io.data_sram_en <> ms.io.data_sram_en
    io.data_sram_we <> ms.io.data_sram_we
    io.data_sram_addr <> ms.io.data_sram_waddr
    io.data_sram_wdata <> ms.io.data_sram_wdata

    io.debug_wb_pc <> ws.io.debug_wb_pc
    io.debug_wb_rf_we <> ws.io.debug_wb_rf_we
    io.debug_wb_rf_wnum <> ws.io.debug_wb_rf_wnum
    io.debug_wb_rf_wdata <> ws.io.debug_wb_rf_wdata
}

object main extends App {
    emitVerilog(new Top(), Array("--target-dir", "wave"))
    println("ok!")
}