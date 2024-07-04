package pipeline

import chisel3._
import chisel3.util._
import const.Parameters._

object InstBufferConst {
  val IB_LENGTH = 8
}

// TODO: 需要多端口！
class InstBuffer extends Queue(UInt(DATA_WIDTH.W), InstBufferConst.IB_LENGTH) {}
