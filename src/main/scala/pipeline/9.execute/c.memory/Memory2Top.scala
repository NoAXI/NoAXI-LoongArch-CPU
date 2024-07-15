package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.MemOpType
import isa.FuncType

class Memory2TopIO extends SingleStageBundle {
  val dCache      = new Mem2DCacheIO
  val storeBuffer = DecoupledIO(new BufferInfo)
  val forward     = Flipped(new ForwardInfoIO)
  val awake       = Output(new AwakeInfo)
}

class Memory2Top extends Module {
  val io = IO(new Memory2TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush && !raw._1.actualStore)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  val mem2  = Module(new Memory2Access).io

  val isMem   = info.func_type === FuncType.mem
  val isStore = !MemOpType.isread(info.op_type) && isMem || info.actualStore
  val isLoad  = !isStore && isMem

  val loadData           = WireDefault(0.U(DATA_WIDTH.W))
  val storeBufferHit     = info.storeBufferHit && isLoad
  val storeBufferHitData = info.storeBufferHitData
  val storeBufferHitStrb = info.storeBufferHitStrb
  val storeBufferFull    = !io.storeBuffer.ready

  val prevAwake = !storeBufferHit && io.dCache.prevAwake

  // D-Cache
  io.dCache.request.valid       := valid && (info.actualStore || isLoad) && !info.bubble
  io.dCache.request.bits.addr   := info.pa
  io.dCache.request.bits.cached := info.cached
  io.dCache.request.bits.wdata  := info.wdata
  io.dCache.request.bits.wstrb  := info.wmask
  io.dCache.rwType              := isStore
  io.dCache.hitVec              := info.dcachehitVec
  io.dCache.answer.ready        := true.B

  // load
  when(io.dCache.answer.fire) {
    when(storeBufferHit) {
      val cacheRes = io.dCache.answer.bits
      val bitMask  = Cat((3 to 0 by -1).map(i => Fill(8, storeBufferHitStrb(i))))
      loadData := writeMask(bitMask, cacheRes, storeBufferHitData)
    }.otherwise {
      loadData := io.dCache.answer.bits
    }
  }
  mem2.rdata      := loadData
  mem2.addr       := info.pa
  mem2.op_type    := info.op_type
  res.rdInfo.data := mem2.data

  // store
  io.storeBuffer.valid := false.B
  io.storeBuffer.bits  := 0.U.asTypeOf(new BufferInfo)
  when(!storeBufferFull && isStore && !info.actualStore) {
    io.storeBuffer.valid                   := valid && !info.bubble
    io.storeBuffer.bits.valid              := true.B
    io.storeBuffer.bits.requestInfo.cached := info.cached
    io.storeBuffer.bits.requestInfo.addr   := info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
    io.storeBuffer.bits.requestInfo.wdata  := info.wdata
    io.storeBuffer.bits.requestInfo.wstrb  := info.wmask
  }
  when(io.flush) {
    io.storeBuffer.valid := false.B
  }

  // ld but bufferhit, D-Cache dont care
  busy := (!io.dCache.answer.fire && (info.actualStore || isLoad)) || (storeBufferFull && isStore && !info.actualStore)

  io.awake.valid := valid && info.iswf && io.to.fire
  io.awake.preg  := info.rdInfo.preg

  doForward(io.forward, res, valid)
  flushWhen(raw._1, io.flush && !info.actualStore)
  io.to.bits := res

  if (Config.debug_on) {
    dontTouch(loadData)
  }
}
