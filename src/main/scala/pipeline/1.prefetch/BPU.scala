package pipeline

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import const._

object predictConst {
  val INDEX_LENGTH   = 6
  val INDEX_WIDTH    = 1 << INDEX_LENGTH
  val HISTORY_LENGTH = 3
  val HISTORY_WIDTH  = 1 << HISTORY_LENGTH
  val COUNTER_LENGTH = 2
  val COUNTER_WIDTH  = 1 << COUNTER_LENGTH
}

import predictConst._

class PredictInfo extends Bundle {
  val en   = Bool()
  val addr = UInt(ADDR_WIDTH.W)
}
class BpuTrain extends Bundle {
  val en      = Bool()
  val succeed = Bool()
  val real    = Bool()
  val target  = UInt(ADDR_WIDTH.W)
  val index   = UInt(INDEX_LENGTH.W)
}

class BPUIO extends Bundle {
  val pc    = Input(UInt(ADDR_WIDTH.W))
  val res   = Output(new PredictInfo)
  val train = Input(new BpuTrain)
}

class BPU extends Module {
  val io = new BPUIO

  // TODO: Complete the logic
}
