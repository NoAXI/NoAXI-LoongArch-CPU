package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import const.Parameters._
import func.Functions._

// basic info for single pipeline
class SingleInfo extends Bundle {
  val bubble = Bool()

  // basic
  val pc       = UInt(ADDR_WIDTH.W)
  val pc_add_4 = UInt(ADDR_WIDTH.W)
  val inst     = UInt(INST_WIDTH.W)

  // decoded inst
  val func_type = FuncType()
  val op_type   = UInt(5.W) // the maximum number of op_type
  val isload    = Bool()

  // data
  // TODO: I wonder why did you use both src1 & rjData
  val imm    = UInt(DATA_WIDTH.W)
  val src1   = UInt(DATA_WIDTH.W)
  val src2   = UInt(DATA_WIDTH.W)
  val rjData = UInt(DATA_WIDTH.W)
  val rdData = UInt(DATA_WIDTH.W)

  // write reg
  val iswf   = Bool()
  val result = UInt(DATA_WIDTH.W)

  // rename info
  val opreg = UInt(PREG_WIDTH.W)
  val rjMap = new RenameBundle
  val rkMap = new RenameBundle
  val rdMap = new RenameBundle
  val robId = UInt(ROB_WIDTH.W)

  // write csr
  val csr_iswf  = Bool()
  val csr_wmask = UInt(DATA_WIDTH.W)
  val csr_addr  = UInt(CSR_WIDTH.W)
  val csr_value = UInt(DATA_WIDTH.W)

  // exception
  val exc_type  = ECodes()
  val exc_vaddr = UInt(ADDR_WIDTH.W)

  // branch predict
  val predict = new br
}

class DualInfo extends Bundle {
  val bits = Vec(ISSUE_WIDTH, new SingleInfo)
}

class BusyInfo extends Bundle {
  val info = Vec(ISSUE_WIDTH, Bool())
}

class StageBundle extends Bundle {
  val from  = Flipped(DecoupledIO(new DualInfo))
  val to    = DecoupledIO(new DualInfo)
  val flush = Input(Bool())
}

// this is wire(read-only), don't use it as reg
// class ConnectInfo extends Bundle {
//   val infoVec     = new DualInfo
//   val validSignal = Bool()
// }
