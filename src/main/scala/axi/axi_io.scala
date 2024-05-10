package axi

import chisel3._

class AXI_IO extends Bundle {
  val aclock = Input(Clock())
  val areset = Input(Bool())

  // ar
  val arid    = Output(UInt(4.W))
  val araddr  = Output(UInt(32.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))
  val arlock  = Output(UInt(2.W))
  val arcache = Output(UInt(4.W))
  val arprot  = Output(UInt(3.W))
  val arvalid = Bool()
  val arready = Input(Bool())

  // r
  val rid   = Input(UInt(4.W))
  val rdata = Input(UInt(64.W))
//   val rresp  = Input(UInt(2.W))
//   val rlast  = Input(UInt(1.W))
  val rvalid = Input(Bool())
  val rready = Output(Bool())

  // aw
  val awid    = Output(UInt(4.W))
  val awaddr  = Output(UInt(32.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))
  val awlock  = Output(UInt(2.W))
  val awcache = Output(UInt(4.W))
  val awprot  = Output(UInt(3.W))
  val awvalid = Output(Bool())
  val awready = Input(Bool())

  // w
  val wid    = Output(UInt(4.W))
  val wdada  = Output(UInt(64.W))
  val wstrb  = Output(UInt(8.W))
  val wlast  = Output(UInt(1.W))
  val wvalid = Output(Bool())
  val wready = Input(Bool())

  // b
//   val bid    = Input(UInt(4.W))
//   val bresp  = Input(UInt(2.W))
  val bvalid = Input(Bool())
  val bready = Output(Bool())
}
