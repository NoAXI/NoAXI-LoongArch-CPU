package bundles

import chisel3._
import chisel3.util._

import csr._
import const.Parameters._

class GRWrite extends Bundle {
  val we    = Bool()
  val waddr = UInt(AREG_WIDTH.W)
  val wdata = UInt(DATA_WIDTH.W)
}

class CSRWrite extends Bundle {
  val we    = Bool()
  val wmask = UInt(DATA_WIDTH.W)
  val waddr = UInt(CSR_WIDTH.W)
  val wdata = UInt(DATA_WIDTH.W)
}
