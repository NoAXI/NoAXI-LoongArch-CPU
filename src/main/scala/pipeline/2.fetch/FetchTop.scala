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
  flushWhen(info, io.flush)
  val res = WireDefault(info)

  // tlb
  io.tlb.va     := info.pc
  io.tlb.hitVec := info.hitVec
  val pa        = Mux(info.isDirect, info.pa, io.tlb.pa)
  val cached    = io.tlb.cached
  val exception = io.tlb.exception // TODO: two inst exception judge

  // I-Cache
  io.iCache.cached        := cached
  io.iCache.request.bits  := pa
  io.iCache.request.valid := ShiftRegister(io.from.fire, 1) && !exception.en
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready
  val wholeInstVec = io.iCache.answer.bits
  val instVec =
    Mux(info.pc(3), VecInit(wholeInstVec(2), wholeInstVec(3)), VecInit(wholeInstVec(0), wholeInstVec(1)))
  busy.info(0) := !io.iCache.answer.fire

  // exception
  val isADEF  = info.pc(1, 0) =/= "b00".U
  val excType = Mux(isADEF, ECodes.ADEF, Mux(exception.en, exception.excType, ECodes.NONE))
  val excEn   = isADEF || exception.en

  res.instGroup := Mux(info.pc(2), VecInit(instVec(1), instVec(0)), instVec)
  res.fetchExc  := VecInit(excType, excType)
  flushWhen(res, io.flush)

  io.to.bits         := 0.U.asTypeOf(new DualInfo)
  io.to.bits.bits(0) := res
}
