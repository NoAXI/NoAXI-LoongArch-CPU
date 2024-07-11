package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.MemOpType

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
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  val mem2  = Module(new Memory2Access).io

  val isStore = !MemOpType.isread(info.op_type)

  val loadData = WireDefault(0.U(DATA_WIDTH.W))
  mem2.rdata   := loadData
  mem2.addr    := info.pa
  mem2.op_type := info.op_type

  val storeBufferHit     = res.storeBufferHit && isStore
  val storeBufferHitData = res.storeBufferHitData
  val storeBufferFull    = !io.storeBuffer.ready

  val prevAwake = !storeBufferHit && io.dCache.prevAwake

  // D-Cache
  io.dCache.request.valid       := valid
  io.dCache.request.bits.addr   := info.pa
  io.dCache.request.bits.cached := info.cached
  io.dCache.request.bits.wdata  := info.wdata
  io.dCache.request.bits.wstrb  := info.wmask
  io.dCache.rwType              := isStore
  io.dCache.hitVec              := res.dcachehitVec
  io.dCache.answer.ready        := true.B

  // load
  when(storeBufferHit) {
    loadData := storeBufferHitData
  }.elsewhen(io.dCache.answer.fire) {
    loadData := io.dCache.answer.bits
  }
  res.rdInfo.data := mem2.data

  // store
  io.storeBuffer.valid := false.B
  io.storeBuffer.bits  := 0.U.asTypeOf(new BufferInfo)
  when(!storeBufferFull && isStore) {
    io.storeBuffer.valid                   := valid
    io.storeBuffer.bits.valid              := true.B
    io.storeBuffer.bits.requestInfo.cached := info.cached
    io.storeBuffer.bits.requestInfo.addr   := info.pa
    io.storeBuffer.bits.requestInfo.wdata  := info.wdata
    io.storeBuffer.bits.requestInfo.wstrb  := info.wmask
  }

  // ld but bufferhit, D-Cache dont care
  when(!(storeBufferHit && !isStore)) {
    busy := (!io.dCache.answer.fire && !storeBufferHit) || (storeBufferFull && isStore)
  }

  io.awake.valid := valid && info.iswf && io.to.fire
  io.awake.preg  := info.rdInfo.preg

  doForward(io.forward, res, valid)
  io.to.bits := res
}
