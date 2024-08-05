package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.MemOpType
import isa.FuncType

class Mem2BufferIO extends Bundle {
  val forwardpa   = Output(UInt(ADDR_WIDTH.W))
  val forwardHit  = Input(Bool())
  val forwardData = Input(UInt(DATA_WIDTH.W))
  val forwardStrb = Input(UInt((DATA_WIDTH / 8).W))
}

class Memory2TopIO extends SingleStageBundle {
  val dCache           = new Mem2DCacheIO
  val storeBufferWrite = DecoupledIO(new BufferInfo)
  val storeBufferRead  = new Mem2BufferIO
  val mem1             = Flipped(new Mem1Mem2ForwardIO)
  val forward          = Flipped(new ForwardInfoIO)

  val cacOpInfo = Output(new CacOpInfo)
}

class Memory2Top extends Module {
  val io = IO(new Memory2TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush, true)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)

  // I-Cache
  io.cacOpInfo    := info.writeInfo.requestInfo.cacop
  io.cacOpInfo.en := info.actualStore && info.writeInfo.requestInfo.cacop.isICache && info.writeInfo.requestInfo.cacop.en

  val cacStop = RegInit(false.B)
  when(io.cacOpInfo.en) {
    cacStop := true.B
  }
  when(cacStop) {
    io.cacOpInfo.en := false.B
  }
  when(io.from.fire) {
    cacStop := false.B
  }

  val isMem        = info.func_type === FuncType.mem
  val isFirstStore = isMem && MemOpType.iswrite(info.op_type)
  val isFirstLoad  = isMem && MemOpType.isread(info.op_type)
  val isFirstCacop = isMem && info.op_type === MemOpType.cacop
  val isFirstAtom  = isMem && MemOpType.isatom(info.op_type)

  val dcacheEn      = ((info.actualStore && !io.cacOpInfo.en) || (isFirstLoad && info.cached && !isFirstAtom)) && info.canrequest
  val storebufferEn = isFirstStore || (isFirstLoad && !info.cached) || isFirstCacop || isFirstAtom

  val storeBufferFull = !io.storeBufferWrite.ready

  // D-Cache
  io.dCache.request.valid       := valid && dcacheEn && !info.bubble
  io.dCache.request.bits.addr   := info.pa
  io.dCache.request.bits.cached := info.cached
  io.dCache.request.bits.wdata  := info.wdata
  io.dCache.request.bits.wstrb  := info.wmask
  io.dCache.request.bits.rbType := DontCare
  io.dCache.request.bits.atom   := DontCare
  io.dCache.request.bits.cacop  := info.writeInfo.requestInfo.cacop
  io.dCache.rwType              := Mux(info.actualStore, info.writeInfo.requestInfo.rbType, isFirstStore)
  io.dCache.flush               := io.flush
  io.dCache.answer.ready        := true.B

  // storebuffer write
  io.storeBufferWrite.valid := false.B
  io.storeBufferWrite.bits  := 0.U.asTypeOf(new BufferInfo)
  when(storebufferEn && !info.actualStore) {
    io.storeBufferWrite.valid                   := valid && !info.bubble
    io.storeBufferWrite.bits.valid              := true.B
    io.storeBufferWrite.bits.requestInfo.cached := info.cached
    io.storeBufferWrite.bits.requestInfo.addr   := Mux(isFirstStore || isFirstCacop, info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W), info.pa)
    io.storeBufferWrite.bits.requestInfo.wdata  := info.wdata
    io.storeBufferWrite.bits.requestInfo.wstrb  := Mux(isFirstStore, info.wmask, info.op_type)
    io.storeBufferWrite.bits.requestInfo.rbType := isFirstStore
    io.storeBufferWrite.bits.requestInfo.atom   := MemOpType.isatom(info.op_type)
    io.storeBufferWrite.bits.requestInfo.cacop.en   := isFirstCacop
    io.storeBufferWrite.bits.requestInfo.cacop.addr := Mux(isFirstCacop, info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W), 0.U((22 - ROB_WIDTH).W) ## info.robId ## info.rdInfo.areg ## info.rdInfo.preg)
    io.storeBufferWrite.bits.requestInfo.cacop.code := info.rdInfo.areg
  }
  when(io.flush) {
    io.storeBufferWrite.valid := false.B
  }

  // get forward
  io.storeBufferRead.forwardpa := info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
  res.ldData                   := io.dCache.answer.bits
  res.forwardHitVec(0)         := io.mem1.actualStore && io.mem1.addr === info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
  res.forwardHitVec(1)         := io.storeBufferRead.forwardHit
  res.forwardData(0)           := io.mem1.data
  res.forwardData(1)           := io.storeBufferRead.forwardData
  res.forwardStrb(0)           := io.mem1.strb
  res.forwardStrb(1)           := io.storeBufferRead.forwardStrb

  // ld but bufferhit, D-Cache dont care
  busy := ((!io.dCache.answer.fire && dcacheEn)
    || (storeBufferFull && storebufferEn && !info.actualStore) && info.exc_type === ECodes.NONE)

  doForward(io.forward, res, false.B)
  io.to.bits := res

  if (Config.debug_on) {
    val sbvalid = valid
    dontTouch(sbvalid)
  }
}
