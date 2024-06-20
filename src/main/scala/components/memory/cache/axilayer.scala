package memory.cache

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class AXILayerIO extends Bundle {
  val icache = Flipped(new iCache_AXI)
  val dcache = Flipped(new dCache_AXI)
  val to     = new AXI_IO
}

class AXILayer extends Module {
  val io = IO(new AXILayerIO)

  val r_sel       = io.to.r.bits.id(0)
  val ar_sel_lock = RegInit(false.B)
  val ar_sel_val  = RegInit(false.B)
  val ar_sel      = Mux(ar_sel_lock, ar_sel_val, io.dcache.ar.valid)

  if (Config.debug_on) {
    dontTouch(io.to)
  }

  when(io.to.ar.valid) {
    when(io.to.ar.ready) {
      ar_sel_lock := false.B
    }.otherwise {
      ar_sel_lock := true.B
      ar_sel_val  := ar_sel
    }
  }

  io.to.ar    <> io.icache.ar
  io.icache.r <> io.to.r
  io.dcache.r <> io.to.r
  io.to.aw    <> io.dcache.aw
  io.to.w     <> io.dcache.w
  io.dcache.b <> io.to.b

  io.to.aw.bits.addr := io.dcache.aw.bits.addr
  // io.to.aw.bits.addr := io.dcache.aw.bits.addr & 0x1fffffff.U
  io.to.ar.bits.id   := Mux(ar_sel, io.dcache.ar.bits.id, io.icache.ar.bits.id)
  io.to.ar.bits.addr := Mux(ar_sel, io.dcache.ar.bits.addr, io.icache.ar.bits.addr)
  // io.to.ar.bits.addr := Mux(ar_sel, io.dcache.ar.bits.addr, io.icache.ar.bits.addr) & 0x1fffffff.U
  io.to.ar.bits.size := Mux(ar_sel, io.dcache.ar.bits.size, io.icache.ar.bits.size)
  io.to.ar.bits.len  := Mux(ar_sel, io.dcache.ar.bits.len, io.icache.ar.bits.len)
  io.to.ar.valid     := Mux(ar_sel, io.dcache.ar.valid, io.icache.ar.valid)
  io.icache.ar.ready := io.to.ar.ready && !ar_sel
  io.dcache.ar.ready := io.to.ar.ready && ar_sel

  io.to.r.ready     := Mux(r_sel, io.dcache.r.ready, io.icache.r.ready)
  io.icache.r.valid := io.to.r.valid && !r_sel
  io.dcache.r.valid := io.to.r.valid && r_sel
}
