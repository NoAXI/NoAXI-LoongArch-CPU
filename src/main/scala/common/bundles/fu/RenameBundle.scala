package bundles

import chisel3._
import chisel3.util._

import const._
import const.Parameters._

class RenameBundle extends Bundle {
  val valid = Bool()
  val areg  = UInt(AREG_WIDTH.W)
  val preg  = UInt(PREG_WIDTH.W)
}

class RenameBundleIO extends Bundle {
  val valid = Input(Bool())
  val areg  = Input(UInt(AREG_WIDTH.W))
  val preg  = Output(UInt(PREG_WIDTH.W))
}
