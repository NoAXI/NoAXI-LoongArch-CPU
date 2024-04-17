package CPU

import chisel3._
import chisel3.util._

import Parameters._
import HandShake._

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

    val iftop = Module(new IFtop)
    val idtop = Module(new IDtop)
    val ietop = Module(new IEtop)
    val imtop = Module(new IMtop)
    val iwtop = Module(new IWtop)

    val empty_bus = WireInit(0.U.asTypeOf(new Bus))

    iftop.io.hand_shake_bf.valid_out := true.B
    iftop.io.hand_shake_bf.bus_out := empty_bus
    iftop.io.inst_sram_rdata <> io.inst_sram_rdata
    iftop.io.br_bus <> idtop.io.br_bus
    iftop.io.hand_shake_af <> idtop.io.hand_shake_bf

    idtop.io.hand_shake_bf <> iftop.io.hand_shake_af
    idtop.io.rf_bus <> iwtop.io.rf_bus
    idtop.io.hand_shake_af <> ietop.io.hand_shake_bf

    ietop.io.hand_shake_bf <> idtop.io.hand_shake_af
    ietop.io.hand_shake_af <> imtop.io.hand_shake_bf

    imtop.io.hand_shake_bf <> ietop.io.hand_shake_af
    imtop.io.data_sram_rdata <> io.data_sram_rdata
    imtop.io.hand_shake_af <> iwtop.io.hand_shake_bf
    
    iwtop.io.hand_shake_bf <> imtop.io.hand_shake_af
    iwtop.io.hand_shake_af.ready_in := true.B

    io.inst_sram_en <> iftop.io.inst_sram_en
    io.inst_sram_we <> iftop.io.inst_sram_we
    io.inst_sram_addr <> iftop.io.inst_sram_addr
    io.inst_sram_wdata <> iftop.io.inst_sram_wdata

    io.data_sram_en <> ietop.io.data_sram_en
    io.data_sram_we <> ietop.io.data_sram_we
    io.data_sram_addr <> ietop.io.data_sram_addr
    io.data_sram_wdata <> ietop.io.data_sram_wdata

    io.debug_wb_pc <> iwtop.io.debug_wb_pc
    io.debug_wb_rf_we <> iwtop.io.debug_wb_rf_we
    io.debug_wb_rf_wnum <> iwtop.io.debug_wb_rf_wnum
    io.debug_wb_rf_wdata <> iwtop.io.debug_wb_rf_wdata
}

object main extends App {
    emitVerilog(new Top(), Array("--target-dir", "wave"))
    println("ok!")
}