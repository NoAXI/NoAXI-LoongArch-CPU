package stages

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._
import const.ECodes
import axi.iCacheIO

class FetchTopIO extends StageBundle {
  val br     = Input(new br)
  val iCache = new fetch_iCache_IO
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)

  val pc_reg = Module(new PC).io
  pc_reg.br := io.br
  pc_reg.en := io.from.fire

  io.iCache.request.bits  := pc_reg.next_pc
  io.iCache.request.valid := io.from.fire
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready

  busy := !io.iCache.answer.fire

  val is_adef = pc_reg.pc(1, 0) =/= "b00".U

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.pc        := pc_reg.pc
  to_info.inst      := Mux(is_adef, 0.U, io.iCache.answer.bits)
  to_info.exc_type  := Mux(is_adef, ECodes.ADEF, ECodes.NONE)
  to_info.exc_vaddr := pc_reg.pc
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble
}
