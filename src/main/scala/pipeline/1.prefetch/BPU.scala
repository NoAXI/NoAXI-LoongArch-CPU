package pipeline

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import const._
import func.Functions._
import memory.cache._

// BTB
// ===================================================
// |53       34|33           2|1        1|0        0|
// |    tag    | branchTarget |  isCALL  | isReturn |
// ===================================================

object predictConst {
  val INDEX_LENGTH   = 6
  val INDEX_WIDTH    = 1 << INDEX_LENGTH
  val HISTORY_LENGTH = 3
  val HISTORY_WIDTH  = 1 << HISTORY_LENGTH
  val COUNTER_LENGTH = 2
  val COUNTER_WIDTH  = 1 << COUNTER_LENGTH

  // BTB
  val BTB_INDEX_LENGTH = 10
  val BTB_INDEX_WIDTH  = 1 << BTB_INDEX_LENGTH

  val BTB_TAG_LENGTH  = ADDR_WIDTH - BTB_INDEX_LENGTH - 2
  val BTB_FLAG_LENGTH = 2
  val BTB_INFO_LENGTH = BTB_TAG_LENGTH + 32 + BTB_FLAG_LENGTH

  // RAS
  val RAS_DEPTH = 8
  val RAS_WIDTH = log2Ceil(RAS_DEPTH)
}

import predictConst._

class BpuTrain extends Bundle {
  val en       = Bool()
  val succeed  = Bool() // predict succeed
  val real     = Bool() // branch succeed
  val target   = UInt(ADDR_WIDTH.W)
  val pc       = UInt(ADDR_WIDTH.W)
  val isCALL   = Bool() // is call or PC-relative branch
  val isReturn = Bool()

  def index    = pc(INDEX_LENGTH + 3, 4)
  def BTBIndex = pc(BTB_INDEX_LENGTH + 3, 4)
  def BTBTag   = pc(ADDR_WIDTH - 1, ADDR_WIDTH - BTB_TAG_LENGTH)
}

class BPUIO extends Bundle {
  val preFetch = Flipped(new PreFetchBPUIO)
  val fetch    = Flipped(new FetchBPUIO)
}

class BPU extends Module {
  val io = IO(new BPUIO)

  val pFF :: pF :: pTT :: pT :: Nil = Enum(COUNTER_WIDTH)

  val BHT = RegInit(VecInit(Seq.fill(INDEX_WIDTH)(0.U(HISTORY_LENGTH.W))))
  val PHT = RegInit(VecInit(Seq.fill(HISTORY_WIDTH)(pF)))
  val BTB = Module(new xilinx_simple_dual_port_1_clock_ram_write_first(BTB_INFO_LENGTH, BTB_INDEX_WIDTH)).io
  val RAS = RegInit(VecInit(Seq.fill(RAS_DEPTH)(0x1c000000.U(32.W))))

  val top       = RegInit(0.U(RAS_WIDTH.W))
  val top_add_1 = top + 1.U

  BTB.clka  := clock
  BTB.addrb := io.preFetch.pc(BTB_INDEX_LENGTH + 3, 4)
  BTB.wea   := false.B
  BTB.addra := 0.U
  BTB.dina  := 0.U

  val tag      = BTB.doutb(BTB_INFO_LENGTH - 1, BTB_INFO_LENGTH - BTB_TAG_LENGTH)
  val tar      = BTB.doutb(BTB_INFO_LENGTH - BTB_TAG_LENGTH - 1, 2)
  val isCALL   = BTB.doutb(1)
  val isReturn = BTB.doutb(0)
  val RASRest  = top =/= RAS_DEPTH.U

  // train, just work one time
  when(io.preFetch.train.en) {
    val index = (BHT(io.preFetch.train.index) << 1)(HISTORY_LENGTH - 1, 0) | io.preFetch.train.real
    BHT(io.preFetch.train.index) := index
    switch(PHT(index)) {
      is(pFF) {
        PHT(index) := Mux(io.preFetch.train.real, pF, pFF)
      }
      is(pF) {
        PHT(index) := Mux(io.preFetch.train.real, pT, pFF)
      }
      is(pT) {
        PHT(index) := Mux(io.preFetch.train.real, pTT, pF)
      }
      is(pTT) {
        PHT(index) := Mux(io.preFetch.train.real, pTT, pT)
      }
    }
    BTB.wea   := true.B
    BTB.addra := io.preFetch.train.BTBIndex
    BTB.dina := Cat(
      io.preFetch.train.BTBTag,
      io.preFetch.train.target,
      io.preFetch.train.isCALL,
      io.preFetch.train.isReturn,
    )
  }

  // when meet CALL, update the RAS
  when(isCALL && !io.fetch.stall) {
    top      := top_add_1
    RAS(top) := io.fetch.pc_add_4 // the pc is wrong!!
  }

  // direction prediction
  val index = ShiftRegister(BHT(io.preFetch.pc(INDEX_LENGTH + 1, 2)), 1)
  io.fetch.res.en := PHT(index)(1).asBool && tag === io.fetch.pc(ADDR_WIDTH - 1, ADDR_WIDTH - BTB_TAG_LENGTH)

  // target prediction
  io.fetch.res.tar := Mux(isReturn, RAS(top), tar)

  // count
  if (Config.statistic_on) {
    val tot_time     = RegInit(0.U(20.W))
    val succeed_time = RegInit(0.U(20.W))
    when(io.preFetch.train.en) {
      tot_time := tot_time + 1.U
      when(io.preFetch.train.succeed) {
        succeed_time := succeed_time + 1.U
      }
    }
    dontTouch(tot_time)
    dontTouch(succeed_time)
  }
}
