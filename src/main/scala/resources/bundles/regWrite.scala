package bundles

import chisel3._
import chisel3.util._

import const.Parameters._

class GRWrite extends Bundle {
  val we    = Bool()
  val wmask = UInt(DATA_WIDTH.W)
  val waddr = UInt(REG_WIDTH.W)
  val wdata = UInt(DATA_WIDTH.W)
}
