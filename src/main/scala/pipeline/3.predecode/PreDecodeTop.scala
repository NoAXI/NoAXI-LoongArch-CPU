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
  val busy = WireDefault(false.B)
  val from = stageConnect(io.from, io.to, busy, io.flush)

  val info = WireDefault(from._1.bits(0))
  val res  = WireDefault(info)

  val imm16 = VecInit.tabulate(FETCH_DEPTH)(i => SignedExtend(Cat(info.instGroup(i)(25, 10), Fill(2, 0.U)), DATA_WIDTH))
  val imm26 =
    VecInit.tabulate(FETCH_DEPTH)(i => SignedExtend(Cat(info.instGroup(i)(9, 0), info.instGroup(i)(25, 10), Fill(2, 0.U)), DATA_WIDTH))

  val predictFailed = WireDefault(false.B)
  val isbr          = VecInit.tabulate(FETCH_DEPTH)(i => info.instGroup(i)(30))
  val isBBL         = VecInit.tabulate(FETCH_DEPTH)(i => info.instGroup(i)(28, 27) === "b10".U)
  val isJIRL        = VecInit.tabulate(FETCH_DEPTH)(i => isbr(i) && info.instGroup(i)(29, 28) === "b00".U)
  val isCALL        = VecInit.tabulate(FETCH_DEPTH)(i => isbr(i) && (info.instGroup(i)(28, 26) === "b101".U || isJIRL(i) && info.instGroup(i)(4, 0) === 1.U))
  val isReturn      = VecInit.tabulate(FETCH_DEPTH)(i => isbr(i) && info.instGroup(i) === RETURN_ADDR.U)
  val pcGroup       = VecInit(info.pc, info.pc_add_4)
  val immGroup      = VecInit.tabulate(FETCH_DEPTH)(i => Mux(info.instGroup(i)(28, 27) === "b10".U, imm26(i), imm16(i)))
  val tar           = VecInit.tabulate(FETCH_DEPTH)(i => pcGroup(i) + immGroup(i))
  val notbrTar      = VecInit.tabulate(FETCH_DEPTH)(i => nextPC(pcGroup(i)))

  io.predictRes := 0.U.asTypeOf(new PredictRes)
  io.flushapply := false.B
  // if B or BL, but predict not jump
  when(isbr(0) && isBBL(0) && info.instGroupValid(0)) {
    when(!info.predict.en || info.predict.en && info.jumpInst) {
      predictFailed               := true.B
      io.flushapply               := true.B
      io.predictRes.isbr          := true.B
      io.predictRes.br.en         := true.B
      io.predictRes.br.tar        := tar(0)
      io.predictRes.realDirection := true.B
      io.predictRes.pc            := pcGroup(0)
      io.predictRes.isCALL        := isCALL(0)
      io.predictRes.isReturn      := isReturn(0)
      res.instGroupValid(1)       := false.B
      res.jumpInst                := false.B
      res.predict.en              := true.B
      res.predict.tar             := tar(0)
    }
  }.elsewhen(isbr(1) && isBBL(1) && info.instGroupValid(1)) {
    when(!info.predict.en) {
      predictFailed               := true.B
      io.flushapply               := true.B
      io.predictRes.isbr          := true.B
      io.predictRes.br.en         := true.B
      io.predictRes.br.tar        := tar(1)
      io.predictRes.realDirection := true.B
      io.predictRes.pc            := pcGroup(1)
      io.predictRes.isCALL        := isCALL(1)
      io.predictRes.isReturn      := isReturn(1)
      res.predict.en              := true.B
      res.predict.tar             := tar(1)
      res.jumpInst                := true.B
    }
  }

  when(isbr(0) && info.instGroupValid(0) && info.predict.en && !info.jumpInst) {
    res.instGroupValid(1) := false.B
  }

  // you don't know which inst caused

  // when(isbr(0) && !isJIRL(0) && info.predict.en && info.instGroupValid(0)) {
  //   // predict jump but tar wrong
  //   when(tar(0) =/= info.predict.tar) {
  //     predictFailed               := true.B
  //     io.flushapply               := true.B
  //     io.predictRes.isbr          := false.B
  //     io.predictRes.br.en         := true.B
  //     io.predictRes.br.tar        := tar(0)
  //     io.predictRes.realDirection := true.B
  //     io.predictRes.pc            := pcGroup(0)
  //     // res.instGroupValid(1)       := false.B
  //     res.predict.tar := tar(0)
  //   }
  // }.elsewhen(isbr(1) && !isJIRL(1) && info.predict.en && tar(1) =/= info.predict.tar && info.instGroupValid(1)) {
  //   predictFailed               := true.B
  //   io.flushapply               := true.B
  //   io.predictRes.isbr          := false.B
  //   io.predictRes.br.en         := true.B
  //   io.predictRes.br.tar        := tar(1)
  //   io.predictRes.realDirection := true.B
  //   io.predictRes.pc            := pcGroup(1)
  //   res.predict.tar             := tar(1)
  // }

  // TODO: branch jump, first meet?

  // when not jump but predict jump
  // val instNotJumpVec = VecInit.tabulate(FETCH_DEPTH)(i => !isbr(i) && info.instGroupValid(i) || !info.instGroupValid(i))
  // when(instNotJumpVec.reduce(_ && _) && info.predict.en) {
  //   predictFailed               := true.B
  //   io.flushapply               := true.B
  //   io.predictRes.isbr          := false.B
  //   io.predictRes.br.en         := true.B
  //   io.predictRes.br.tar        := notbrTar(0)
  //   io.predictRes.realDirection := false.B
  //   io.predictRes.pc            := pcGroup(0)
  //   res.predict.en              := false.B
  // }

  val flushStop = RegInit(false.B)
  val brStop    = RegInit(false.B)
  when(io.flushapply) {
    flushStop := true.B
    brStop    := true.B
  }

  when(flushStop || info.bubble) {
    io.flushapply := false.B
  }

  when(brStop || info.bubble) {
    io.predictRes.br.en := false.B
  }

  when(io.from.fire) {
    brStop    := false.B
    flushStop := false.B
  }

  io.to.bits         := from._1
  io.to.bits.bits(0) := res

  if (Config.debug_on) {
    dontTouch(res)
    dontTouch(predictFailed)
    dontTouch(isbr)
    // dontTouch(instNotJumpVec)
  }
}
