package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import const.Parameters._

class info extends Bundle {
  val bubble = Bool()
  val pc     = UInt(ADDR_WIDTH.W)
  val inst   = UInt(INST_WIDTH.W)

  val func_type = FuncType()
  val op_type   = UInt(5.W) // the maximum number of op_type
  val isload    = Bool()
  val ld_tag    = Bool()

  val imm  = UInt(DATA_WIDTH.W)
  val src1 = UInt(DATA_WIDTH.W)
  val src2 = UInt(DATA_WIDTH.W)
  val rj   = UInt(DATA_WIDTH.W)
  val rd   = UInt(DATA_WIDTH.W)

  val iswf   = Bool()
  val wfreg  = UInt(REG_WIDTH.W)
  val result = UInt(DATA_WIDTH.W)

  val csr_iswf   = Bool()
  val csr_addr  = UInt(CSR_WIDTH.W)

  val exc_type  = ECodes()
  val exc_vaddr = UInt(ADDR_WIDTH.W)
}

class StageBundle extends Bundle {
  val from        = Flipped(DecoupledIO(new info))
  val to          = DecoupledIO(new info)
  val flush       = Input(Bool())
  val flush_apply = Output(Bool())
}
