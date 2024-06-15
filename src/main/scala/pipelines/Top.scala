package stages

import chisel3._
import chisel3.util._

import axi._
import csr._
import bundles._
import controller._
import const.Parameters._
import mmu.TLB

class sramTopIO extends Bundle {
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

class axiTopIO extends Bundle {
  val axi = new AXI_IO

  val debug_wb_pc       = Output(UInt(32.W))
  val debug_wb_rf_we    = Output(UInt(4.W))
  val debug_wb_rf_wnum  = Output(UInt(5.W))
  val debug_wb_rf_wdata = Output(UInt(32.W))
}

class Top extends Module {
  val io = IO(new axiTopIO)

  val fetch     = Module(new FetchTop).io
  val decoder   = Module(new DecoderTop).io
  val execute   = Module(new ExecuteTop).io
  val memory    = Module(new MemoryTop).io
  val writeback = Module(new WriteBackTop).io
  val flusher   = Module(new Flusher).io
  val forwarder = Module(new Forwarder).io
  val csr       = Module(new CSR).io
  val icache    = Module(new iCache).io
  val dcache    = Module(new dCache).io
  val axilayer  = Module(new AXILayer).io
  val tlb       = Module(new TLB).io

  // axi
  io.axi          <> axilayer.to
  axilayer.icache <> icache.axi
  axilayer.dcache <> dcache.axi

  // handshake
  fetch.from.valid   := RegNext(!reset.asBool) & !reset.asBool
  fetch.from.bits    := WireDefault(0.U.asTypeOf(new info))
  fetch.to           <> decoder.from
  decoder.to         <> execute.from
  execute.to         <> memory.from
  memory.to          <> writeback.from
  writeback.to.ready := true.B

  // flusher
  flusher.apply := Seq(
    fetch.flush_apply,
    decoder.flush_apply,
    execute.flush_apply,
    memory.flush_apply,
    writeback.flush_apply,
  )
  fetch.flush     := flusher.flush(0)
  decoder.flush   := flusher.flush(1)
  execute.flush   := flusher.flush(2)
  memory.flush    := flusher.flush(3)
  writeback.flush := flusher.flush(4)

  // forward
  forwarder.dataIn := Seq(
    writeback.forward_data,
    memory.forward_data,
    execute.forward_data,
  )
  forwarder.load_complete := memory.load_complete
  forwarder.forward_query := decoder.forward_query
  decoder.forward_ans     := forwarder.forward_ans

  // csr
  csr.exc_happen         := writeback.exc_happen
  execute.br_exc         := csr.br_exc
  csr.csr_write          := writeback.csr_write
  csr.csr_reg_read       <> decoder.csr_reg_read
  writeback.flush_by_csr := csr.flush_by_csr

  // tlb
  tlb.csr <> csr.tlb
  tlb.mem <> memory.tlb

  // predict
  fetch.predict_result  <> decoder.predict_result
  decoder.predict_check <> execute.predict_check

  // fetch
  fetch.iCache <> icache.fetch
  fetch.br     := execute.br

  // decoder
  decoder.gr_write := writeback.gr_write

  // execute
  execute.dcache <> dcache.exe

  // memory
  memory.dCache <> dcache.mem

  // writeback
  io.debug_wb_pc       := writeback.debug_wb.pc
  io.debug_wb_rf_we    := writeback.debug_wb.rf_we
  io.debug_wb_rf_wnum  := writeback.debug_wb.rf_wnum
  io.debug_wb_rf_wdata := writeback.debug_wb.rf_wdata
}
