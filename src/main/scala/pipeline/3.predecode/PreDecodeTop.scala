package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.PipelineType

class PreDecodeTopIO extends StageBundle {
  val predictRes = Output(new PredictRes)
  val flushapply = Output(Bool())
}

// check predict
// TODO: 逻辑有点混乱，到底要check哪些东西？两条指令之间的valid关系？
class PreDecodeTop extends Module {
  val io   = IO(new PreDecodeTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info = WireDefault(from._1.bits(0))
  val res  = WireDefault(info)

  val imm16 = VecInit.tabulate(FETCH_DEPTH)(i => SignedExtend(Cat(info.instGroup(i)(25, 10), Fill(2, 0.U)), DATA_WIDTH))
  val imm26 =
    VecInit.tabulate(FETCH_DEPTH)(i =>
      SignedExtend(Cat(info.instGroup(i)(9, 0), info.instGroup(i)(25, 10), Fill(2, 0.U)), DATA_WIDTH),
    )

  val predictFailed = WireDefault(false.B)
  val isbr          = VecInit.tabulate(FETCH_DEPTH)(i => info.instGroup(i)(30))
  val isBBL         = VecInit.tabulate(FETCH_DEPTH)(i => info.instGroup(i)(28, 27) === "b10".U)
  val isJIRL        = VecInit.tabulate(FETCH_DEPTH)(i => info.instGroup(i)(29, 28) === "b00".U)
  val pcGroup       = VecInit(info.pc, info.pc_add_4)
  val immGroup      = VecInit.tabulate(FETCH_DEPTH)(i => Mux(info.instGroup(i)(28, 27) === "b10".U, imm26(i), imm16(i)))
  val tar           = VecInit.tabulate(FETCH_DEPTH)(i => pcGroup(i) + immGroup(i))

  io.predictRes := 0.U.asTypeOf(new PredictRes)
  io.flushapply := false.B
  // if B or BL, but predict not jump
  when(isbr(0) && isBBL(0) && !info.predict.en && info.instGroupValid(0)) {
    predictFailed               := true.B
    io.flushapply               := true.B
    io.predictRes.isbr          := true.B
    io.predictRes.br.en         := true.B
    io.predictRes.br.tar        := tar(0)
    io.predictRes.realDirection := true.B
    io.predictRes.pc            := pcGroup(0)
    res.instGroupValid(1)       := false.B
    res.predict.en              := true.B
    res.predict.tar             := tar(0)
  }.elsewhen(isbr(1) && isBBL(1) && !info.predict.en && info.instGroupValid(1)) {
    predictFailed               := true.B
    io.flushapply               := true.B
    io.predictRes.isbr          := true.B
    io.predictRes.br.en         := true.B
    io.predictRes.br.tar        := tar(1)
    io.predictRes.realDirection := true.B
    io.predictRes.pc            := pcGroup(1)
    res.predict.en              := true.B
    res.predict.tar             := tar(1)
  }

  when(isbr(0) && info.predict.en) {
    res.instGroupValid(1) := false.B
  }

  when(isbr(0) && !isJIRL(0) && info.predict.en && tar(0) =/= info.predict.tar) {
    // predict jump but tar wrong
    predictFailed               := true.B
    io.flushapply               := true.B
    io.predictRes.isbr          := false.B
    io.predictRes.br.en         := true.B
    io.predictRes.br.tar        := tar(0)
    io.predictRes.realDirection := true.B
    io.predictRes.pc            := pcGroup(0)
    // res.instGroupValid(1)       := false.B
    res.predict.tar := tar(0)
  }.elsewhen(isbr(1) && !isJIRL(1) && info.predict.en && tar(1) =/= info.predict.tar) {
    predictFailed               := true.B
    io.flushapply               := true.B
    io.predictRes.isbr          := false.B
    io.predictRes.br.en         := true.B
    io.predictRes.br.tar        := tar(1)
    io.predictRes.realDirection := true.B
    io.predictRes.pc            := pcGroup(1)
    res.predict.tar             := tar(1)
  }

  // TODO: branch jump, first meet?

  // TODO：not br, but predict jump

  io.to.bits := 0.U.asTypeOf(new DualInfo)
  flushWhen(res, io.flush)
  io.to.bits.bits(0) := res

  if (Config.debug_on) {
    dontTouch(isbr)
  }
}
