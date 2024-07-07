package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class Memory1TopIO extends SingleStageBundle {
  val dCache = new Mem1DCacheIO
}

class Memory1Top extends Module {
  val io = IO(new Memory1TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)

  val has_exc = info.exc_type =/= ECodes.NONE

  val dcache_saved_ans = RegInit(0.U(DATA_WIDTH.W))
  val complete         = RegInit(false.B)
  when(io.dCache.answer.fire) {
    dcache_saved_ans := io.dCache.answer.bits
    complete         := true.B
  }
  when(io.from.fire) {
    complete         := false.B
    dcache_saved_ans := 0.U
  }
  val finish = io.dCache.answer.fire || complete
  val mem    = Module(new Mmu).io
  mem.op_type  := info.op_type
  mem.result   := info.pa
  mem.rd_value := info.rdInfo.data
  val mem_has_exc = mem.exc_type =/= ECodes.NONE
  io.dCache.request.valid := (mem.data_sram.en || mem.data_sram.we.orR) && !has_exc && !mem_has_exc
  when(ShiftRegister(info.pc, 1) === info.pc && ShiftRegister(finish, 1)) {
    // when getans and this stage is stall
    io.dCache.request.valid := false.B
  }
  io.dCache.request.bits.cached := info.cached
  io.dCache.request.bits.re     := mem.data_sram.en
  io.dCache.request.bits.we     := mem.data_sram.we.orR
  io.dCache.request.bits.addr   := info.pa
  io.dCache.request.bits.data   := mem.data_sram.wdata
  io.dCache.request.bits.strb   := mem.data_sram.we
  mem.data_sram.rdata           := Mux(io.dCache.answer.fire, io.dCache.answer.bits, dcache_saved_ans)
  io.dCache.answer.ready        := true.B
  busy                          := !finish && !mem_has_exc
  when(io.dCache.answer_imm) { busy := false.B }

  res.result := mem.data
  flushWhen(res, io.flush)
  io.to.bits := res
}
