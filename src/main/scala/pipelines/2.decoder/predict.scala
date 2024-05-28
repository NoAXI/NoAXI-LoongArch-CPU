package stages

import chisel3._
import chisel3.util._

import bundles._
import Funcs.Functions._
import const.predictConst._
import const.Parameters._

class PredictIO extends Bundle {
  val pc     = Input(UInt(ADDR_WIDTH.W))
  val inst   = Input(UInt(INST_WIDTH.W))
  val result = Output(new br)
  val check  = Input(new brCheck)
}

class Predict extends Module {
  val io = IO(new PredictIO)

  val pFF :: pF :: pTT :: pT :: Nil = Enum(COUNTER_WIDTH)

  val BHR = RegInit(VecInit(Seq.fill(INDEX_WIDTH)(0.U(HISTORY_LENGTH.W))))
  val PHT = RegInit(VecInit(Seq.fill(HISTORY_WIDTH)(pT)))

  when(io.check.en) {
    val index = (BHR(io.check.index) << 1)(HISTORY_LENGTH - 1, 0) | io.check.real
    BHR(io.check.index) := index
    switch(PHT(index)) {
      is(pFF) {
        PHT(index) := Mux(io.check.real, pF, pFF)
      }
      is(pF) {
        PHT(index) := Mux(io.check.real, pT, pFF)
      }
      is(pT) {
        PHT(index) := Mux(io.check.real, pTT, pF)
      }
      is(pTT) {
        PHT(index) := Mux(io.check.real, pTT, pT)
      }
    }
  }

  // count
  val tot_time = RegInit(0.U(20.W))
  val succeed_time = RegInit(0.U(20.W))
  when(io.check.en) {
    tot_time := tot_time + 1.U
    when(io.check.succeed) {
      succeed_time := succeed_time + 1.U
    }
  }
  dontTouch(tot_time)
  dontTouch(succeed_time)

  val isbr  = io.inst(30).asBool
  val index = BHR(io.pc(INDEX_LENGTH + 1, 2))
  val pht_value = PHT(index)(1).asBool
  dontTouch(isbr)
  dontTouch(pht_value)
  io.result.en  := PHT(index)(1).asBool && isbr
  io.result.tar := io.pc + SignedExtend(Cat(io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  // must failed at jirl, its jumpaddr = rj + imm
}
