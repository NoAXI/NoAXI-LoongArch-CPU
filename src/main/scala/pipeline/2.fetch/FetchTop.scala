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
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(false.B)
  val from = stageConnect(io.from, io.to, busy, io.flush)

  val info                    = WireDefault(from._1.bits(0))
  val res                     = WireDefault(info)
  val force_stop_for_simulate = WireDefault(false.B)

  if (Config.debug_on) {
    val pcend = VecInit(
      0x1c000104.U,
      0x1c000108.U,
      0x1c00010c.U,
      0x1c000110.U,
      0x1c000114.U,
      0x1c000118.U,
    )
    when(pcend.contains(info.pc)) {
      force_stop_for_simulate := true.B
    }
  }

  // tlb
  io.tlb.va     := info.pc
  io.tlb.hitVec := info.hitVec

  val stall         = !ShiftRegister(io.from.fire, 1)
  val savedCached   = RegInit(false.B)
  val savedPa       = RegInit(0.U(ADDR_WIDTH.W))
  val savedExc      = RegInit(0.U.asTypeOf(new ExcInfo))
  val savedPredict  = RegInit(0.U.asTypeOf(new BranchInfo))
  val savedJumpInst = RegInit(false.B)
  when(ShiftRegister(io.from.fire, 1)) {
    savedCached   := io.tlb.cached
    savedPa       := io.tlb.pa
    savedExc      := io.tlb.exception
    savedPredict  := io.bpu.predict
    savedJumpInst := io.bpu.firstInstJump
  }
  val tlbpa     = Mux(stall, savedPa, io.tlb.pa)
  val tlbcached = Mux(stall, savedCached, io.tlb.cached)
  val tlbexc    = Mux(stall, savedExc, io.tlb.exception)
  val pa        = Mux(info.actualStore, info.writeInfo.requestInfo.addr, tlbpa)
  val cached    = Mux(info.actualStore, info.writeInfo.requestInfo.cached, tlbcached)
  val exception = tlbexc

  // exception
  val isADEF  = info.pc(1, 0) =/= "b00".U
  val excType = Mux(isADEF, ECodes.ADEF, Mux(exception.en, exception.excType, ECodes.NONE))
  val excEn   = isADEF || exception.en

  // I-Cache
  io.iCache.cached        := cached
  io.iCache.request.bits  := pa
  io.iCache.request.valid := ShiftRegister(io.from.fire, 1) && !excEn && !info.bubble && !force_stop_for_simulate
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready
  val wholeInstVec = io.iCache.answer.bits
  val instVec =
    Mux(info.pc(3), VecInit(wholeInstVec(2), wholeInstVec(3)), VecInit(wholeInstVec(0), wholeInstVec(1)))
  busy := !io.iCache.answer.fire && !excEn && !info.bubble && !force_stop_for_simulate

  res.instGroup := Mux(info.pc(2), VecInit(instVec(1), instVec(0)), instVec)
  // !!! TODO: uncached fetch logic !!!
  // when(!cached) {
  //   res.instGroupValid(1) := false.B
  // }
  res.fetchExc := VecInit(excType, excType)

  res.predict  := Mux(stall, savedPredict, io.bpu.predict)
  res.jumpInst := !Mux(stall, savedJumpInst, io.bpu.firstInstJump)

  io.predict := Mux(stall, savedPredict, io.bpu.predict)
  when(info.bubble || !io.from.fire) {
    io.predict.en := false.B
  }

  io.to.bits         := from._1
  io.to.bits.bits(0) := res
}
