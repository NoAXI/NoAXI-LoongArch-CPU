package bundles

import chisel3._
import chisel3.util._

import const.Parameters._
import const._

class ROBInfo extends Bundle {
  val done      = Bool()
  val debug_pc  = UInt(ADDR_WIDTH.W)
  val wen       = Bool()
  val areg      = UInt(AREG_WIDTH.W)
  val preg      = UInt(PREG_WIDTH.W)
  val opreg     = UInt(PREG_WIDTH.W)
  val exc_type  = ECodes()
  val exc_vaddr = UInt(ADDR_WIDTH.W)
}

class RenameBundle {
  val areg = UInt(AREG_WIDTH.W)
  val preg = UInt(PREG_WIDTH.W)
}