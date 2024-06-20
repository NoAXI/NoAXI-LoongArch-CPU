package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import const.Parameters._

// basic info for single pipeline
class info extends Bundle {
  val bubble   = Bool()
  val pc       = UInt(ADDR_WIDTH.W)
  val pc_add_4 = UInt(ADDR_WIDTH.W)
  val inst     = UInt(INST_WIDTH.W)

  val func_type = FuncType()
  val op_type   = UInt(5.W) // the maximum number of op_type
  val isload    = Bool()
  // val ld_tag    = Bool()

  val imm  = UInt(DATA_WIDTH.W)
  val src1 = UInt(DATA_WIDTH.W)
  val src2 = UInt(DATA_WIDTH.W)
  val rj   = UInt(DATA_WIDTH.W)
  val rd   = UInt(DATA_WIDTH.W)

  // write areg info
  val iswf   = Bool()
  val wfreg  = UInt(AREG_WIDTH.W)
  val result = UInt(DATA_WIDTH.W)

  // csr write info
  val csr_iswf  = Bool()
  val csr_wmask = UInt(DATA_WIDTH.W)
  val csr_addr  = UInt(CSR_WIDTH.W)
  val csr_value = UInt(DATA_WIDTH.W)

  // exception
  val exc_type  = ECodes()
  val exc_vaddr = UInt(ADDR_WIDTH.W)

  // branch predict
  val predict = new br

  val tlb_hit = new Bundle {
    val inst = UInt(tlbConst.TLB_ENTRIES.W)
    val data = UInt(tlbConst.TLB_ENTRIES.W)
  }
}

// dual issue info
class ConnectInfo extends Bundle {
  val info         = Vec(ISSUE_WIDTH, new info)
  val valid_signal = Bool()
}

class StageBundle extends Bundle {
  val from        = Flipped(DecoupledIO(new ConnectInfo))
  val to          = DecoupledIO(new ConnectInfo)
  val flush       = Input(Bool())
  val flush_apply = Output(Bool())
}

// signle issue info
// class FullInfo extends Bundle {
//   val info         = new info
//   val valid_signal = Bool()
// }

// class StageBundle extends Bundle {
//   val from        = Flipped(DecoupledIO(new info))
//   val to          = DecoupledIO(new info)
//   val flush       = Input(Bool())
//   val flush_apply = Output(Bool())
// }
