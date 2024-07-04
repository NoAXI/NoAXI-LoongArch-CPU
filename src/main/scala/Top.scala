package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import const.Parameters._

import csr._
import controller._

import memory.tlb._
import memory.cache._

class TopIO extends Bundle {
  val axi               = new AXI_IO
  val debug_wb_pc       = Output(UInt(32.W))
  val debug_wb_rf_we    = Output(UInt(4.W))
  val debug_wb_rf_wnum  = Output(UInt(5.W))
  val debug_wb_rf_wdata = Output(UInt(32.W))
}

class Top extends Module {
  val io = IO(new TopIO)

  // frontend pipeline
  val rename = Module(new RenameTop).io
  val issue  = Module(new IssueTop).io

  // rename
  val rat  = Module(new Rat).io
  val rob  = Module(new Rob).io
  val preg = Module(new PReg).io

  // backend pipeline
  val readreg = Module(new ReadRegTop).io

  // backend ctrl
  val forward = Module(new Forward).io

  // memory access
  val axilayer = Module(new AXILayer).io
  val icache   = Module(new iCache).io
  val dcache   = Module(new dCache_with_cached_writebuffer).io

  // rename <> rat
  rename.ratRename <> rat.rename
  rename.ratRead   <> rat.read
  rename.ratFull   <> rat.full

  // rename <> rob
  rename.rob     <> rob.rename
  rename.robFull <> rob.full

  // axi
  io.axi          <> axilayer.to
  axilayer.icache <> icache.axi
  axilayer.dcache <> dcache.axi
}