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
  val forwardStrb = Input(UInt((DATA_WIDTH / 8).W))
}

class Memory1TopIO extends SingleStageBundle {
  val tlb         = new Stage1TLBIO
  val dCache      = new Mem1DCacheIO
  val storeBuffer = new Mem1BufferIO
  val awake       = Output(new AwakeInfo)
  val readreg     = Flipped(new ReadRegMem1ForwardIO)
  val mem0        = Flipped(new Mem0Mem1ForwardIO)
}

class Memory1Top extends Module {
  val io   = IO(new Memory1TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush && !raw._1.actualStore)

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
  io.storeBuffer.forwardpa := pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
  val bfhit     = io.readreg.actualStore && io.readreg.addr === pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
  val bfhit1    = io.mem0.actualStore && io.mem0.addr === pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
  val bfdata    = io.readreg.data
  val bfdata1   = io.mem0.data
  val bfstrb    = io.readreg.strb
  val bfstrb1   = io.mem0.strb
  val rhit      = io.storeBuffer.forwardHit
  val rdata     = io.storeBuffer.forwardData
  val rstrb     = io.storeBuffer.forwardStrb
  val savedHit  = RegInit(false.B)
  val savedData = RegInit(0.U(DATA_WIDTH.W))
  val savedStrb = RegInit(0.U((DATA_WIDTH / 8).W))
  when(!io.to.ready && !savedHit) {
    savedHit  := rhit
    savedData := rdata
    savedStrb := rstrb
  }
  when(io.from.fire) {
    savedHit  := false.B
    savedData := 0.U
    savedStrb := 0.U
  }
  res.storeBufferHit     := savedHit || rhit || bfhit
  res.storeBufferHitData := Mux(rhit, rdata, Mux(bfhit, bfdata, Mux(savedHit, savedData, bfdata1)))
  res.storeBufferHitStrb := Mux(rhit, rstrb, Mux(bfhit, bfstrb, Mux(savedHit, savedStrb, bfstrb1)))

  // D-Cache
  io.dCache.addr := pa
  val hitVec = io.dCache.hitVec
  res.dcachehitVec := hitVec

  io.awake.valid := valid && info.iswf && io.to.fire && hitVec.reduce(_ || _)
  io.awake.preg  := info.rdInfo.preg
  flushWhen(raw._1, io.flush && !info.actualStore)
  io.to.bits := res
}
