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
  val arvalid = Output(Bool())
  val arready = Input(Bool())

  // r
  val rid    = Input(UInt(4.W))
  val rdata  = Input(UInt(64.W))
  val rresp  = Input(UInt(2.W)) // don't care
  val rlast  = Input(UInt(1.W)) // don't care
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
  val wdata  = Output(UInt(64.W))
  val wstrb  = Output(UInt(8.W))
  val wlast  = Output(UInt(1.W))
  val wvalid = Output(Bool())
  val wready = Input(Bool())

  // b
  val bid    = Input(UInt(4.W)) // don't care
  val bresp  = Input(UInt(2.W)) // don't care
  val bvalid = Input(Bool())
  val bready = Output(Bool())
}

class AR extends Bundle {
  val addr  = UInt(32.W)
  val len   = UInt(8.W)
  val size  = UInt(3.W)
  val valid = Bool()
}

class R extends Bundle {
  val ready = Bool()
}

class AW extends Bundle {
  val addr  = UInt(32.W)
  val len   = UInt(8.W)
  val size  = UInt(3.W)
  val valid = Bool()
}

class W extends Bundle {
  val data  = UInt(32.W)
  val strb  = UInt(4.W)
  val valid = Bool()
}

class B extends Bundle {
  val ready = Bool()
}

class AXI_ICache_IO extends Bundle {
  val ar      = Output(new AR)
  val arready = Input(Bool())

  val r      = Output(new R)
  val rvalid = Input(Bool())
  val rdata  = Input(UInt(32.W))
}

class AXI_DCache_IO extends Bundle {
  val ar      = Output(new AR)
  val arready = Input(Bool())

  val r      = Output(new R)
  val rvalid = Input(Bool())
  val rdata  = Input(UInt(32.W))

  val aw      = Output(new AW)
  val awready = Input(Bool())

  val w      = Output(new W)
  val wready = Input(Bool())

  val b      = Output(new B)
  val bvalid = Input(Bool())
}
