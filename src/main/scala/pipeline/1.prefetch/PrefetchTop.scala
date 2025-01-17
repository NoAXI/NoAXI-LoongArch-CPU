package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class PrefetchTopIO extends StageBundle {
  val iCache                  = new PreFetchICacheIO
  val tlb                     = new Stage0TLBIO
  val bpu                     = new PreFetchBPUIO
  val predictResFromFront     = Input(new PredictRes)
  val predictResFromBack      = Input(new PredictRes)
  val predictResFromPredictor = Input(new BranchInfo)
  val flushTarget             = Input(new BranchInfo)
}

class PrefetchTop extends Module {
  val io   = IO(new PrefetchTopIO)
  val busy = WireDefault(false.B)
  val from = stageConnect(io.from, io.to, busy, io.flush)

  val info = WireDefault(from._1.bits(0))
  val res  = WireDefault(0.U.asTypeOf(new SingleInfo))

  val pc       = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val pc_add_4 = pc + 4.U
  val pc_add_8 = pc + 8.U

  // Mux(io.predictResFromBack.br.en, io.predictResFromBack, io.predictResFromFront)
  val predictRes = io.predictResFromFront
  val flushRes   = io.flushTarget

  // pc
  val (flushHappen, flushPC)     = (flushRes.en, flushRes.tar)
  val (predictFailed, exactPC)   = (predictRes.br.en, predictRes.br.tar)
  val (predictHappen, predictPC) = (io.predictResFromPredictor.en, io.predictResFromPredictor.tar)

  val lastIsBranch = RegInit(false.B)

  when(io.from.fire) {
    pc           := nextPC(pc)
    lastIsBranch := false.B
  }
  when(predictHappen && !lastIsBranch) {
    pc := predictPC
  }
  when(predictFailed) {
    pc           := exactPC
    lastIsBranch := true.B
  }
  when(flushHappen) {
    pc           := flushPC
    lastIsBranch := true.B
  }

  val invalid = predictHappen && !predictFailed && !flushHappen && !lastIsBranch

  // bpu
  io.bpu.pcValid  := VecInit(Seq(true.B, pc_add_4(2)))
  io.bpu.pcGroup  := VecInit(Seq(pc, pc_add_4))
  io.bpu.npcGroup := VecInit(Seq(pc_add_4, pc_add_8))
  io.bpu.train    := Mux(io.predictResFromBack.isbr, io.predictResFromBack, io.predictResFromFront)
  io.bpu.valid    := io.to.fire && !invalid

  // tlb
  io.tlb.va       := pc
  io.tlb.memType  := memType.fetch
  io.tlb.unitType := false.B
  val hitVec   = io.tlb.hitVec

  // I-Cache
  io.iCache.request.valid     := io.from.fire
  io.iCache.request.bits.addr := pc

  io.to.bits         := 0.U.asTypeOf(new DualInfo)
  res.pc             := pc
  res.pc_add_4       := pc_add_4
  res.hitVec         := hitVec
  res.instGroupValid := VecInit(Seq(true.B, pc_add_4(2)))
  io.to.bits.bits(0) := Mux(invalid, info.getFlushInfo, res)
}
