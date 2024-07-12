package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class Mem1BufferIO extends Bundle {
  val forwardpa   = Output(UInt(ADDR_WIDTH.W))
  val forwardHit  = Input(Bool())
  val forwardData = Input(UInt(DATA_WIDTH.W))
}

class Memory1TopIO extends SingleStageBundle {
  val tlb         = new Stage1TLBIO
  val dCache      = new Mem1DCacheIO
  val storeBuffer = new Mem1BufferIO
  val awake       = Output(new AwakeInfo)
}

class Memory1Top extends Module {
  val io   = IO(new Memory1TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  val mem1  = Module(new Memory1Access).io

  // tlb
  io.tlb.va     := info.va
  io.tlb.hitVec := info.hitVec
  val pa        = Mux(info.actualStore, info.writeInfo.requestInfo.addr, Mux(info.isDirect, info.pa, io.tlb.pa))
  val cached    = Mux(info.actualStore, info.writeInfo.requestInfo.cached, io.tlb.cached)
  val exception = io.tlb.exception
  res.pa     := pa
  res.cached := cached

  // mem1
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

  // StoreBuffer
  io.storeBuffer.forwardpa := pa
  val rhit  = io.storeBuffer.forwardHit
  val rdata = io.storeBuffer.forwardData
  res.storeBufferHit     := rhit
  res.storeBufferHitData := rdata

  // D-Cache
  io.dCache.addr := pa
  val hitVec = io.dCache.hitVec
  res.dcachehitVec := hitVec

  io.awake.valid := valid && info.iswf && io.to.fire && hitVec.reduce(_ || _)
  io.awake.preg  := info.rdInfo.preg

  io.to.bits := res
}
