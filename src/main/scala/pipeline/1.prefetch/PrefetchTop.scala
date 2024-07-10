package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class PrefetchTopIO extends StageBundle {
  val iCache        = new PreFetchICacheIO
  val tlb           = new Stage0TLBIO
  val bpu           = new PreFetchBPUIO
  val predictRes    = Input(new PredictRes) // 预测结果
  val exceptionJump = Input(new br)         // 异常跳转
}

class PrefetchTop extends Module {
  val io   = IO(new PrefetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info = WireDefault(from._1.bits(0))
  flushWhen(info, io.flush)
  val res = WireDefault(0.U.asTypeOf(new SingleInfo))

  val pc       = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val pc_add_4 = pc + 4.U
  val pc_add_8 = pc + 8.U

  // bpu
  io.bpu.stall    := !io.from.fire
  io.bpu.pcValid  := VecInit(Seq(true.B, pc_add_4(2)))
  io.bpu.pcGroup  := VecInit(Seq(pc, pc_add_4))
  io.bpu.npcGroup := VecInit(Seq(pc_add_4, pc_add_8))
  io.bpu.train    := io.predictRes

  // pc
  val (predictFailed, exactPC) = (io.predictRes.br.en, io.predictRes.br.tar)
  val (excHappen, excPC)       = (io.exceptionJump.en, io.exceptionJump.tar)
  val (predictEn, predictPC)   = (io.bpu.nextPC.en, io.bpu.nextPC.tar)
  when(io.from.fire) {
    pc := MuxCase(
      nextPC(pc),
      Seq(
        excHappen     -> excPC,
        predictFailed -> exactPC,
        predictEn     -> predictPC,
      ),
    )
  }

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
  res.hitVec         := hitVec
  res.isDirect       := isDirect
  res.pa             := directpa
  res.predict        := io.bpu.nextPC
  res.instGroupValid := io.bpu.pcValid
  flushWhen(res, io.flush)
  io.to.bits.bits(0) := res
}
