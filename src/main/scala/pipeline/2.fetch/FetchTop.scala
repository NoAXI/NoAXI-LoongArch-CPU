package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FetchTopIO extends StageBundle {
  val tlb    = new Stage1TLBIO
  val iCache = new FetchICacheIO
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info = WireDefault(from._1.bits(0))
  val res  = WireDefault(info)

  // tlb
  io.tlb.va     := info.pc
  io.tlb.hitVec := info.hitVec
  val pa        = io.tlb.pa
  val cached    = io.tlb.cached
  val exception = io.tlb.exception // TODO: two inst exception judge

  // I-Cache
  io.iCache.cached        := cached
  io.iCache.request.bits  := pa
  io.iCache.request.valid := io.from.fire && !exception.en
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready
  val wholeInstVec = io.iCache.answer.bits
  val instVec =
    Mux(info.pc(3), VecInit(wholeInstVec(3), wholeInstVec(2)), VecInit(wholeInstVec(1), wholeInstVec(0)))
  busy.info(0) := !io.iCache.answer.fire

  // exception
  val isADEF  = info.pc(1, 0) =/= "b00".U
  val excType = Mux(isADEF, ECodes.ADEF, Mux(exception.en, exception.excType, ECodes.NONE))
  val excEn   = isADEF || exception.en

  res.instGroup := instVec
  res.fetchExc  := VecInit(excType, excType)

  io.to.bits.bits(0) := res
}
