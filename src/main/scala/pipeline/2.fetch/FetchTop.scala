package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FetchTopIO extends StageBundle {
  val tlb     = new Stage1TLBIO
  val iCache  = new FetchICacheIO
  val bpu     = new FetchBPUIO
  val predict = Output(new BranchInfo)
  val busy    = Output(Bool())
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(false.B)
  val from = stageConnect(io.from, io.to, busy, io.flush)
  io.busy := busy

  val info = WireDefault(from._1.bits(0))
  // flushUntilValidWhen(from._1, io.flush, io.to.valid)
  val res = WireDefault(info)

  // tlb
  io.tlb.va     := info.pc
  io.tlb.hitVec := info.hitVec

  val stall       = !ShiftRegister(io.from.fire, 1)
  val savedCached = RegInit(false.B)
  val savedPa     = RegInit(0.U(ADDR_WIDTH.W))
  val savedExc    = RegInit(0.U.asTypeOf(new ExcInfo))
  when(ShiftRegister(io.from.fire, 1)) {
    savedCached := io.tlb.cached
    savedPa     := io.tlb.pa
    savedExc    := io.tlb.exception
  }
  val tlbpa     = Mux(stall, savedPa, io.tlb.pa)
  val tlbcached = Mux(stall, savedCached, io.tlb.cached)
  val tlbexc    = Mux(stall, savedExc, io.tlb.exception)
  val pa        = Mux(info.actualStore, info.writeInfo.requestInfo.addr, Mux(info.isDirect, info.pa, tlbpa))
  val cached    = Mux(info.actualStore, info.writeInfo.requestInfo.cached, tlbcached)
  val exception = tlbexc

  // exception
  val isADEF  = info.pc(1, 0) =/= "b00".U
  val excType = Mux(isADEF, ECodes.ADEF, Mux(exception.en, exception.excType, ECodes.NONE))
  val excEn   = isADEF || exception.en

  // I-Cache
  io.iCache.cached        := cached
  io.iCache.request.bits  := pa
  io.iCache.request.valid := ShiftRegister(io.from.fire, 1) && !excEn && !info.bubble
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready
  val wholeInstVec = io.iCache.answer.bits
  val instVec =
    Mux(info.pc(3), VecInit(wholeInstVec(2), wholeInstVec(3)), VecInit(wholeInstVec(0), wholeInstVec(1)))
  busy := !io.iCache.answer.fire && !excEn && !info.bubble

  res.instGroup := Mux(info.pc(2), VecInit(instVec(1), instVec(0)), instVec)
  res.fetchExc  := VecInit(excType, excType)
  res.predict   := io.bpu.predict

  io.predict := io.bpu.predict
  when(info.bubble || !io.from.fire) {
    io.predict.en := false.B
  }

  io.to.bits         := 0.U.asTypeOf(new DualInfo)
  io.to.bits.bits(0) := res
}
