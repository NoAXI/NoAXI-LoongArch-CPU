package stages

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._
import const.ECodes
// import axi.iCacheIO

class FetchTopIO extends StageBundle {
  val predict_result = Input(new br)
  val br             = Input(new br)
  val iCache         = new fetch_iCache_IO
  // val iCache = new pipeline_Cache_IO
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)

  val br_en      = RegInit(false.B)
  val saved_addr = RegInit(0.U(ADDR_WIDTH.W))

  val pc_reg = Module(new PC).io
  pc_reg.br.en  := WireDefault(false.B)
  pc_reg.br.tar := 0.U
  pc_reg.en     := io.from.fire

  when(br_en) {
    pc_reg.br.en  := true.B
    pc_reg.br.tar := saved_addr
  }.elsewhen(io.br.en || io.predict_result.en) {
    pc_reg.br.en  := true.B
    pc_reg.br.tar := Mux(io.br.en, io.br.tar, io.predict_result.tar)
  }

  when(pc_reg.pc === saved_addr) {
    br_en := false.B
  }

  when(io.br.en || io.predict_result.en) {
    br_en      := true.B
    saved_addr := Mux(io.br.en, io.br.tar, io.predict_result.tar)
  }
  // io.iCache.request.bits.re   := true.B
  // io.iCache.request.bits.we   := false.B
  // io.iCache.request.bits.data := 0.U
  // io.iCache.request.bits.strb := 0.U
  io.iCache.request.bits  := pc_reg.next_pc
  io.iCache.request.valid := ShiftRegister(io.from.fire, 1)
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready

  busy := !io.iCache.answer.fire

  val is_adef = pc_reg.pc(1, 0) =/= "b00".U

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.pc_add_4  := pc_reg.pc_add_4
  to_info.pc        := pc_reg.pc
  to_info.inst      := Mux(is_adef, 0.U, io.iCache.answer.bits)
  to_info.exc_type  := Mux(is_adef, ECodes.ADEF, ECodes.NONE)
  to_info.exc_vaddr := pc_reg.pc
  FlushWhen(to_info, io.flush || br_en)
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble
}
/*
在异常跳转时，因为异常触发时，异常的下一条指令已被取指
所以需要想办法撤销这次取指操作（或者标记取指无效）
解决：这个不需要执行的指令替换为气泡
需要保证撤销的操作不是我需要跳转的异常
需要想办法让异常跳转的指令生效，取指1c008000（它的持续时间很短,io.br.exc_en的持续时间很短）

遇到了axi如果太快，导致之前的某个跳转，未冲刷到后面指令的问题，导致跳转后指令被执行
有a b c三个指令
a是跳转指令，在b取指操作前，需要告诉fetch有跳转
现在的问题是
a是跳转指令，在b取指操作后，才知道fetch有跳转，导致对c取指

计时器异常导致异常跳转，需要撤销之前的跳转操作
 */
