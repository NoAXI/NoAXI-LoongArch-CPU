package stages

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._
import const.ECodes

class FetchTopIO extends StageBundle {
  val br        = Input(new br)
  val br_exc    = Input(new br)
  val inst_sram = new inst_sramIO
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)

  val pc_reg = Module(new PC).io
  pc_reg.br    := Mux(io.br_exc.en, io.br_exc, io.br)
  pc_reg.en    := io.from.fire
  pc_reg.flush := io.flush

  val is_adef = pc_reg.pc(1, 0) =/= "b00".U

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.pc        := pc_reg.pc
  to_info.inst      := io.inst_sram.rdata
  to_info.exc_type  := Mux(is_adef, ECodes.ADEF, ECodes.NONE)
  to_info.exc_vaddr := pc_reg.pc
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.flush_apply     := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble
  io.inst_sram.en    := io.from.fire
  io.inst_sram.we    := 0.U
  io.inst_sram.addr  := pc_reg.next_pc
  io.inst_sram.wdata := 0.U
}
