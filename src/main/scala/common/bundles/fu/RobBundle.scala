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

class ROBReg extends Bundle {
  val reg = RegInit(VecInit(Seq.fill(ROB_NUM)(0.U.asTypeOf(new ROBInfo))))
  def write(index: UInt, data: ROBInfo): Unit = {
    reg(index) := data
  }
  def read(index: UInt): ROBInfo = {
    reg(index).asTypeOf(new ROBInfo)
  }
}
