package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import pipeline._
import const.cacheConst._
import const.Parameters._
import const.tlbConst._
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

  // only used in fetch stage
  val instGroup      = Vec(FETCH_DEPTH, UInt(INST_WIDTH.W))
  val instGroupValid = Vec(FETCH_DEPTH, Bool())
  val fetchExc       = Vec(FETCH_DEPTH, ECodes())

  // decoded inst
  val func_type    = FuncType()
  val op_type      = UInt(OP_TYPE_WIDTH.W) // the maximum number of op_type
  val pipelineType = PipelineType()

  // data info
  val imm        = UInt(DATA_WIDTH.W)
  val src1Ispc   = Output(Bool())
  val src1IsZero = Output(Bool())
  val src2IsFour = Output(Bool())
  val src2IsImm  = Output(Bool())

  // write reg
  val iswf = Bool()

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
  val hasFlush  = Bool() // branch predict fail, exception

  // branch predict
  val predict   = new br
  val realBr    = new br
  val realBrDir = Bool()
  val isCALL    = Bool()
  val isReturn  = Bool()

  // tlb
  val va       = UInt(ADDR_WIDTH.W)
  val pa       = UInt(ADDR_WIDTH.W)
  val cached   = Bool()
  val isDirect = Bool()
  val hitVec   = Vec(TLB_ENTRIES, Bool())

  // mem
  val writeInfo    = new BufferInfo
  val dcachehitVec = Vec(WAY_WIDTH, Bool())
  val wdata        = UInt(DATA_WIDTH.W)
  val wmask        = UInt((DATA_WIDTH / 8).W)
  val actualStore  = Bool()

  // storebuffer
  val storeBufferHit     = Bool()
  val storeBufferHitData = UInt(DATA_WIDTH.W)
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

class SingleStageBundleWithoutFlush extends Bundle {
  val from = Flipped(DecoupledIO(new SingleInfo))
  val to   = DecoupledIO(new SingleInfo)
}
class SingleStageBundle extends SingleStageBundleWithoutFlush {
  val flush = Input(Bool())
}
