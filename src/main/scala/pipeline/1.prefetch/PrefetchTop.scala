package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class PrefetchTopIO extends StageBundle {
  val iCache              = new PreFetchICacheIO
  val tlb                 = new Stage0TLBIO
  val bpu                 = new PreFetchBPUIO
  val predictResFromFront = Input(new PredictRes) // 预测结果
  val predictResFromBack  = Input(new PredictRes) // 预测结果
  val flushTarget         = Input(new BranchInfo)
}

class PrefetchTop extends Module {
  val io   = IO(new PrefetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info = WireDefault(from._1.bits(0))
  flushWhen(from._1, io.flush)
  val res = WireDefault(0.U.asTypeOf(new SingleInfo))

  val pc       = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val pc_add_4 = pc + 4.U
  val pc_add_8 = pc + 8.U

  // Mux(io.predictResFromBack.br.en, io.predictResFromBack, io.predictResFromFront)
  val predictRes = io.predictResFromFront
  val flushRes   = io.flushTarget

  // pc
  val (flushHappen, flushPC)   = (flushRes.en, flushRes.tar)
  val (predictFailed, exactPC) = (predictRes.br.en, predictRes.br.tar)
  val (predictEn, predictPC)   = (io.bpu.nextPC.en, io.bpu.nextPC.tar)
  when(io.from.fire) {
    pc := MuxCase(
      nextPC(pc),
      Seq(
        // excHappen     -> excPC,
        predictFailed -> exactPC,
        predictEn     -> predictPC,
      ),
    )
  }
  when(flushHappen) {
    pc := flushPC
  }

  // bpu
  io.bpu.pcValid  := VecInit(Seq(true.B, pc_add_4(2)))
  io.bpu.pcGroup  := VecInit(Seq(pc, pc_add_4))
  io.bpu.npcGroup := VecInit(Seq(pc_add_4, pc_add_8))
  io.bpu.train    := Mux(io.predictResFromBack.isbr, io.predictResFromBack, io.predictResFromFront)

  // tlb
  io.tlb.va      := pc
  io.tlb.memType := memType.fetch
  val hitVec   = io.tlb.hitVec
  val isDirect = io.tlb.isDirect
  val directpa = io.tlb.directpa

  // I-Cache
  io.iCache.request.valid     := io.from.fire
  io.iCache.request.bits.addr := pc

  io.to.bits         := 0.U.asTypeOf(new DualInfo)
  res.pc             := pc
  res.pc_add_4       := pc_add_4
  res.pc_add_8       := pc_add_8
  res.hitVec         := hitVec
  res.isDirect       := isDirect
  res.pa             := directpa
  res.predict        := io.bpu.nextPC
  res.instGroupValid := VecInit(Seq(true.B, pc_add_4(2)))
  flushWhen(from._1, io.flush)
  io.to.bits.bits(0) := res
}
