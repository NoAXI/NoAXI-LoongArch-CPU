package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.FuncType

class Mem1Mem2ForwardIO extends Bundle {
  val actualStore = Output(Bool())
  val addr        = Output(UInt(ADDR_WIDTH.W))
  val data        = Output(UInt(DATA_WIDTH.W))
  val strb        = Output(UInt((DATA_WIDTH / 8).W))
}

class Memory1TopIO extends SingleStageBundle {
  val tlb    = new Stage1TLBIO
  val dCache = new Mem1DCacheIO
  val mem2   = new Mem1Mem2ForwardIO
}

class Memory1Top extends Module {
  val io   = IO(new Memory1TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush, true)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  val mem1  = Module(new Memory1Access).io

  // tlb
  io.tlb.va     := info.va
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
  res.pa     := pa
  res.cached := cached

  // mem1
  mem1.isMem    := info.func_type === FuncType.mem
  mem1.op_type  := info.op_type
  mem1.addr     := info.va
  mem1.rd_value := info.rkInfo.data
  res.wdata     := Mux(info.actualStore, info.writeInfo.requestInfo.wdata, mem1.wdata)
  res.wmask     := Mux(info.actualStore, info.writeInfo.requestInfo.wstrb, mem1.wmask)

  // exception
  val hasExc   = info.exc_type =/= ECodes.NONE
  val isALE    = mem1.exc_type === ECodes.ALE
  val excType  = Mux(isALE, ECodes.ALE, Mux(exception.en, exception.excType, ECodes.NONE))
  val excVaddr = Mux(isALE, mem1.exc_vaddr, io.tlb.exception.excVAddr)
  val excEn    = isALE || exception.en
  res.exc_type  := Mux(hasExc, info.exc_type, excType)
  res.exc_vaddr := Mux(hasExc, info.exc_vaddr, excVaddr)
  res.iswf      := Mux(excEn, false.B, info.iswf)

  when(info.actualStore) {
    res.exc_type  := ECodes.NONE
    res.exc_vaddr := 0.U
    res.iswf      := false.B
  }

  // forward
  io.mem2.actualStore := info.actualStore
  io.mem2.addr        := info.writeInfo.requestInfo.addr
  io.mem2.data        := info.writeInfo.requestInfo.wdata
  io.mem2.strb        := info.writeInfo.requestInfo.wstrb

  // D-Cache
  io.dCache.addr := Mux(info.actualStore, info.writeInfo.requestInfo.addr, info.va)

  io.to.bits := res

  if (Config.debug_on) {
    dontTouch(valid)
  }
}
