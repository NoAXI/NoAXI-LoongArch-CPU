package pipeline.frontend

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import configs._

object predictConst {
  val INDEX_LENGTH   = 6
  val INDEX_WIDTH    = 1 << INDEX_LENGTH
  val HISTORY_LENGTH = 3
  val HISTORY_WIDTH  = 1 << HISTORY_LENGTH
  val COUNTER_LENGTH = 2
  val COUNTER_WIDTH  = 1 << COUNTER_LENGTH
}

import predictConst._

class PredictInput extends Bundle {
  val en = Input(Bool())
  val pc = Input(UInt(ADDR_WIDTH.W))
}
class PredictInfo extends Bundle {
  val en   = Bool()
  val addr = UInt(ADDR_WIDTH.W)
}
class BpuTrain extends Bundle {
  val en      = Bool()
  val succeed = Bool()
  val real    = Bool()
  val index   = UInt(INDEX_LENGTH.W)
}

class BPU extends Module {
  val io = new Bundle {
    val in    = Input(new PredictInput)
    val res   = Output(new PredictInfo)
    val train = Input(new BpuTrain)
  }

  val pFF :: pF :: pTT :: pT :: Nil = Enum(4)

  // TODO: add btb here
  val BHT = RegInit(VecInit(Seq.fill(INDEX_WIDTH)(0.U(HISTORY_LENGTH.W))))
  val PHT = RegInit(VecInit(Seq.fill(HISTORY_WIDTH)(pT)))

  // train
  val index = (BHT(io.train.index) << 1)(HISTORY_LENGTH - 1, 0) | io.train.real
  BHT(io.train.index) := index
  switch(PHT(index)) {
    is(pFF) {
      PHT(index) := Mux(io.train.real, pF, pFF)
    }
    is(pF) {
      PHT(index) := Mux(io.train.real, pT, pFF)
    }
    is(pT) {
      PHT(index) := Mux(io.train.real, pTT, pF)
    }
    is(pTT) {
      PHT(index) := Mux(io.train.real, pTT, pT)
    }
  }

  // send branch predict to prefetch top
  // val isbr      = io.inst(30).asBool
  // val index     = BHT(io.pc(INDEX_LENGTH + 1, 2))
  // val pht_value = PHT(index)(1).asBool
  // dontTouch(isbr)
  // dontTouch(pht_value)
  // io.result.en  := PHT(index)(1).asBool && isbr
  // io.result.tar := io.pc + SignedExtend(Cat(io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  // must failed at jirl, its jumpaddr = rj + imm

  // return predict result (using btb info)
  // TODO: add correct code here
  io.res.en   := false.B
  io.res.addr := 0.U

  // debug: hit rate statistic
  if (CpuConfig.debug_on) {
    val tot_time     = RegInit(0.U(20.W))
    val succeed_time = RegInit(0.U(20.W))
    when(io.train.en) {
      tot_time := tot_time + 1.U
      when(io.train.succeed) {
        succeed_time := succeed_time + 1.U
      }
    }
    dontTouch(tot_time)
    dontTouch(succeed_time)
  }
}
