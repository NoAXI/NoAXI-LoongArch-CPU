package axi

import chisel3._
import chisel3.util._
import config._

class axilayer_IO extends Bundle {
  val iCache = Flipped(new AXI_ICache_IO)
  val dCache = Flipped(new AXI_DCache_IO)
  val axi    = new AXI_IO
}

class axi extends Module {
  val io = IO(new axilayer_IO)

  // aw
  io.axi.awid       := 1.U
  io.axi.awaddr     := io.dCache.aw.addr
  io.axi.awlen      := io.dCache.aw.len
  io.axi.awsize     := io.dCache.aw.size
  io.axi.awburst    := 1.U
  io.axi.awlock     := 0.U
  io.axi.awcache    := 0.U
  io.axi.awprot     := 0.U
  io.axi.awvalid    := io.dCache.aw.valid
  io.dCache.awready := io.axi.awready
  // w
  io.axi.wid       := 1.U
  io.axi.wdata     := io.dCache.w.data
  io.axi.wstrb     := io.dCache.w.strb
  io.axi.wlast     := 1.U
  io.axi.wvalid    := io.dCache.w.valid
  io.dCache.wready := io.axi.wready
  // b
  io.dCache.bvalid := io.axi.bvalid
  io.axi.bready    := io.dCache.b.ready

  val ar_sel_lock = RegInit(false.B)
  val ar_sel_val  = RegInit(false.B)
  val ar_id       = Mux(ar_sel_lock, ar_sel_val, !io.iCache.ar.valid && io.dCache.ar.valid)

  when(io.axi.arvalid) {
    when(io.axi.arready) {
      ar_sel_lock := false.B
    }.otherwise {
      ar_sel_lock := true.B
      ar_sel_val  := ar_id
    }
  }

  io.axi.arid    := ar_id
  io.axi.araddr  := Mux(ar_id, io.dCache.ar.addr, io.iCache.ar.addr)
  io.axi.arlen   := 0.U
  io.axi.arsize  := Mux(ar_id, io.dCache.ar.size, io.iCache.ar.size)
  io.axi.arburst := 1.U
  io.axi.arlock  := 0.U
  io.axi.arcache := 0.U
  io.axi.arprot  := 0.U
  io.axi.arvalid := Mux(ar_id, io.dCache.ar.valid, io.iCache.ar.valid)
  io.iCache.arready := io.axi.arready && !ar_id
  io.dCache.arready := io.axi.arready && ar_id

  val r_sel = io.axi.rid(0)
  io.iCache.rdata  := io.axi.rdata
  io.iCache.rvalid := io.axi.rvalid && !r_sel
  io.dCache.rdata  := io.axi.rdata
  io.dCache.rvalid := io.axi.rvalid && r_sel
  io.axi.rready    := Mux(r_sel, io.dCache.r.ready, io.iCache.r.ready)
}
