package stages

import chisel3._
import chisel3.util._

import config._
import controller._

class Top_IO extends Bundle with Parameters {
  // inst sram interface
  val inst_sram_en    = Output(Bool())
  val inst_sram_we    = Output(UInt(4.W))
  val inst_sram_addr  = Output(UInt(32.W))
  val inst_sram_wdata = Output(UInt(32.W))
  val inst_sram_rdata = Input(UInt(32.W))

  // data sram interface
  val data_sram_en    = Output(Bool())
  val data_sram_we    = Output(UInt(4.W))
  val data_sram_addr  = Output(UInt(32.W))
  val data_sram_wdata = Output(UInt(32.W))
  val data_sram_rdata = Input(UInt(32.W))

  // trace debug interface
  val debug_wb_pc       = Output(UInt(32.W))
  val debug_wb_rf_we    = Output(UInt(4.W))
  val debug_wb_rf_wnum  = Output(UInt(5.W))
  val debug_wb_rf_wdata = Output(UInt(32.W))
}

class Top extends Module with Parameters {
  val io = IO(new Top_IO)

  val fs   = Module(new IF)
  val ds   = Module(new ID)
  val es   = Module(new IE)
  val ms   = Module(new IM)
  val ws   = Module(new IW)
  val csr  = Module(new CSR)
  val ctrl = Module(new controller)

  fs.io.from.valid      := RegNext(!reset.asBool) & !reset.asBool
  fs.io.from.bits       := RegInit(0.U.asTypeOf(new info))
  fs.io.inst_sram_rdata := io.inst_sram_rdata
  fs.io.br_bus          <> ds.io.br_bus
  fs.io.flush_en        := ctrl.io.flush_en(0)
  fs.io.exc_bus         := csr.io.exc_bus

  ds.io.from        <> fs.io.to
  ds.io.rf_bus      <> ws.io.rf_bus
  ds.io.rcsr_bus    <> ws.io.rcsr_bus
  ds.io.ds_reg_data <> ctrl.io.ds_reg_data
  ds.io.csr_rdata   := csr.io.rdata
  ds.io.flush_en    := ctrl.io.flush_en(1)
  ds.io.has_exc     := es.io.this_exc || ms.io.this_exc || ws.io.this_exc

  es.io.from        <> ds.io.to
  es.io.ds_reg_info := ds.io.ds_reg_info
  es.io.flush_en    := ctrl.io.flush_en(2)
  es.io.has_exc     := ms.io.this_exc || ws.io.this_exc

  ms.io.from            <> es.io.to
  ms.io.data_sram_rdata := io.data_sram_rdata
  ms.io.flush_en        := ctrl.io.flush_en(3)
  ms.io.has_exc         := ws.io.this_exc

  ws.io.from     <> ms.io.to
  ws.io.to.ready := true.B
  ws.io.flush_en := ctrl.io.flush_en(4)

  csr.io.re     := ds.io.csr_re
  csr.io.raddr  := ds.io.csr_raddr
  csr.io.rf_bus := ds.io.rcsr_bus
  csr.io.info   := ws.io.to.bits
  csr.io.start  := ws.io.exc_start
  csr.io.end    := ws.io.exc_end

  io.inst_sram_en    <> fs.io.inst_sram_en
  io.inst_sram_we    <> fs.io.inst_sram_we
  io.inst_sram_addr  <> fs.io.inst_sram_addr
  io.inst_sram_wdata <> fs.io.inst_sram_wdata

  io.data_sram_en    <> es.io.data_sram_en
  io.data_sram_we    <> es.io.data_sram_we
  io.data_sram_addr  <> es.io.data_sram_addr
  io.data_sram_wdata <> es.io.data_sram_wdata

  io.debug_wb_pc       <> ws.io.debug_wb_pc
  io.debug_wb_rf_we    <> ws.io.debug_wb_rf_we
  io.debug_wb_rf_wnum  <> ws.io.debug_wb_rf_wnum
  io.debug_wb_rf_wdata <> ws.io.debug_wb_rf_wdata

  ctrl.io.es             <> es.io.es
  ctrl.io.csr_es         <> es.io.csr_es
  ctrl.io.ms             <> ms.io.ms
  ctrl.io.csr_ms         <> ms.io.csr_ms
  ctrl.io.ws             <> ws.io.ws
  ctrl.io.csr_ws         <> ws.io.csr_ws
  ctrl.io.ds_reg_info    <> ds.io.ds_reg_info
  ctrl.io.flush_apply(0) := fs.io.flush_apply
  ctrl.io.flush_apply(1) := ds.io.flush_apply
  ctrl.io.flush_apply(2) := es.io.flush_apply
  ctrl.io.flush_apply(3) := ms.io.flush_apply
  ctrl.io.flush_apply(4) := ws.io.flush_apply
}

object main extends App {
  emitVerilog(new Top(), Array("--target-dir", "wave"))
  println("ok!")
}
