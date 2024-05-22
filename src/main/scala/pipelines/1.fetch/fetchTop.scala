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

  val next_inst_invalid = RegInit(false.B)
  val exc_br_en         = RegInit(false.B)
  val saved_addr        = RegInit(0.U(ADDR_WIDTH.W))

  val pc_reg = Module(new PC).io
  pc_reg.br     := io.br
  pc_reg.exc_en := io.br.exc_en
  pc_reg.en     := io.from.fire

  when(exc_br_en) {
    pc_reg.br.en  := true.B
    pc_reg.br.tar := saved_addr
  }

  when(io.br.exc_en) {
    next_inst_invalid := true.B
    exc_br_en         := true.B
    saved_addr        := io.br.tar
  }
  val invalid = next_inst_invalid || io.br.exc_en

  io.iCache.request.bits  := pc_reg.next_pc
  io.iCache.request.valid := io.from.fire
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready

  busy := !io.iCache.answer.fire

  when(pc_reg.pc === saved_addr) {
    exc_br_en := false.B
  }

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
  when(invalid && !busy) {
    when(pc_reg.pc =/= saved_addr) {
      to_info        := WireDefault(0.U.asTypeOf(new info))
      to_info.bubble := true.B
    }
    next_inst_invalid := false.B
  }
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble
}
/*
在异常跳转时，因为异常触发时，异常的下一条指令已被取指
所以需要想办法撤销这次取指操作（或者标记取指无效）
解决：这个不需要执行的指令替换为气泡
需要保证撤销的操作不是我需要跳转的异常
需要想办法让异常跳转的指令生效，取指1c008000（它的持续时间很短,io.br.exc_en的持续时间很短）
 */
