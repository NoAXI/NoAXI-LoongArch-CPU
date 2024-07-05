package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import const.Parameters._
import func.Functions._

class InstV extends Bundle {
  val inst  = UInt(INST_WIDTH.W)
  val valid = Bool()
}

class RegInfo extends Bundle {
  val areg = UInt(AREG_WIDTH.W)
  val preg = UInt(PREG_WIDTH.W)
  val data = UInt(DATA_WIDTH.W)
}

// basic info for single pipeline
class SingleInfo extends Bundle {
  val bubble = Bool()

  // basic
  val pc       = UInt(ADDR_WIDTH.W)
  val pc_add_4 = UInt(ADDR_WIDTH.W)
  val inst     = UInt(INST_WIDTH.W)

  // only used between fetch and ib
  val instV    = Vec(FETCH_DEPTH, new InstV)
  val fetchExc = Vec(FETCH_DEPTH, ECodes())

  // decoded inst
  val func_type    = FuncType()
  val op_type      = UInt(OP_TYPE_WIDTH.W) // the maximum number of op_type
  val pipelineType = PipelineType()

  // data info
  val imm = UInt(DATA_WIDTH.W)

  // write reg
  val iswf   = Bool()
  val result = UInt(DATA_WIDTH.W)

  // rename info
  val opreg  = UInt(PREG_WIDTH.W) // the old preg id of rd
  val rjInfo = new RegInfo
  val rkInfo = new RegInfo
  val rdInfo = new RegInfo
  val robId  = UInt(ROB_WIDTH.W)

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

class SingleStageBundle extends Bundle {
  val from  = Flipped(DecoupledIO(new SingleInfo))
  val to    = DecoupledIO(new SingleInfo)
  val flush = Input(Bool())
}
