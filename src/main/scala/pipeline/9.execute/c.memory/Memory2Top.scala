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
  val mem0             = Flipped(new ToMem2ForwardIO)
  val mem1             = Flipped(new ToMem2ForwardIO)
  val forward          = Flipped(new ForwardInfoIO)
}

class Memory2Top extends Module {
  val io = IO(new Memory2TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush, true)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  val mem2  = Module(new Memory2Access).io

  val isMem   = info.func_type === FuncType.mem
  val isStore = !MemOpType.isread(info.op_type) && isMem || info.actualStore
  val isLoad  = !isStore && isMem

  val storeBufferFull = !io.storeBufferWrite.ready

  // D-Cache
  io.dCache.request.valid       := valid && (info.actualStore || isLoad) && !info.bubble
  io.dCache.request.bits.addr   := info.pa
  io.dCache.request.bits.cached := info.cached
  io.dCache.request.bits.wdata  := info.wdata
  io.dCache.request.bits.wstrb  := info.wmask
  io.dCache.rwType              := isStore
  io.dCache.flush               := io.flush
  io.dCache.hitVec              := info.dcachehitVec
  io.dCache.answer.ready        := true.B

  // storebuffer write
  io.storeBufferWrite.valid := false.B
  io.storeBufferWrite.bits  := 0.U.asTypeOf(new BufferInfo)
  when(isStore && !info.actualStore) {
    io.storeBufferWrite.valid                   := valid && !info.bubble
    io.storeBufferWrite.bits.valid              := true.B
    io.storeBufferWrite.bits.requestInfo.cached := info.cached
    io.storeBufferWrite.bits.requestInfo.addr   := info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
    io.storeBufferWrite.bits.requestInfo.wdata  := info.wdata
    io.storeBufferWrite.bits.requestInfo.wstrb  := info.wmask
  }
  when(io.flush) {
    io.storeBufferWrite.valid := false.B
  }

  // get forward
  io.storeBufferRead.forwardpa := info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W)
  val ldData = io.dCache.answer.bits
  val forwardHitVec = VecInit(
    io.mem0.actualStore && io.mem0.addr === info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W),
    io.mem1.actualStore && io.mem1.addr === info.pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W),
    io.storeBufferRead.forwardHit,
  )
  val forwardData = VecInit(
    io.mem0.data,
    io.mem1.data,
    io.storeBufferRead.forwardData,
  )
  val forwardStrb = VecInit(
    io.mem0.strb,
    io.mem1.strb,
    io.storeBufferRead.forwardStrb,
  )
  // merge data
  val bitHit  = WireDefault(VecInit(Seq.fill(4)(0.U(8.W))))
  val bitStrb = WireDefault(VecInit(Seq.fill(4)(false.B)))
  for (i <- 0 until 3) {
    when(forwardHitVec(i)) {
      for (j <- 0 to 3) {
        when(forwardStrb(i)(j)) {
          bitStrb(j) := true.B
          bitHit(j)  := forwardData(i)(j * 8 + 7, j * 8)
        }
      }
    }
  }
  val bitMask   = Cat((3 to 0 by -1).map(i => Fill(8, bitStrb(i))))
  val mergeData = writeMask(bitMask, ldData, bitHit.asUInt)

  // load
  mem2.rdata      := mergeData
  mem2.addr       := info.pa
  mem2.op_type    := info.op_type
  res.rdInfo.data := mem2.data

  // ld but bufferhit, D-Cache dont care
  busy := ((!io.dCache.answer.fire && (info.actualStore || isLoad))
    || (storeBufferFull && isStore && !info.actualStore) && info.exc_type === ECodes.NONE)

  doForward(io.forward, res, valid, busy)
  io.to.bits := res

  if (Config.debug_on) {
    val sbvalid = valid
    dontTouch(sbvalid)
  }
}
