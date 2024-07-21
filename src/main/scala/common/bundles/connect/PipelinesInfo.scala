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

abstract class BasicStageInfo extends Bundle {
  def getFlushInfo: BasicStageInfo
}

// basic info for single pipeline
class SingleInfo extends BasicStageInfo {
  val bubble = Bool()

  // basic
  val pc       = UInt(ADDR_WIDTH.W)
  val pc_add_4 = UInt(ADDR_WIDTH.W)
  val pc_add_8 = UInt(ADDR_WIDTH.W)
  val inst     = UInt(INST_WIDTH.W)

  // only used in fetch stage
  val invalidInst    = Bool()
  val instGroup      = Vec(FETCH_DEPTH, UInt(INST_WIDTH.W))
  val instGroupValid = Vec(FETCH_DEPTH, Bool())
  val fetchExc       = Vec(FETCH_DEPTH, ECodes())

  // decoded inst
  val func_type    = FuncType()
  val op_type      = UInt(OP_TYPE_WIDTH.W) // the maximum number of op_type
  val pipelineType = PipelineType()

  // data info
  val imm        = UInt(DATA_WIDTH.W)
  val src1Ispc   = Bool()
  val src1IsZero = Bool()
  val src2IsFour = Bool()
  val src2IsImm  = Bool()
  val src1       = UInt(DATA_WIDTH.W)
  val src2       = UInt(DATA_WIDTH.W)

  // write reg
  val iswf = Bool()

  // rename info
  val opreg  = UInt(PREG_WIDTH.W) // the old preg id of rd
  val rjInfo = new RegInfo
  val rkInfo = new RegInfo
  val rdInfo = new RegInfo
  val robId  = UInt(ROB_WIDTH.W)

  // csr
  val isWriteCsr = Bool()
  val isReadCsr  = Bool()
  val csr_wmask  = UInt(DATA_WIDTH.W)
  val csr_addr   = UInt(CSR_WIDTH.W)
  val csr_value  = UInt(DATA_WIDTH.W)

  // exception
  val exc_en    = Bool()
  val exc_type  = ECodes()
  val exc_vaddr = UInt(ADDR_WIDTH.W)

  // branch predict
  val predict   = new BranchInfo
  val realBr    = new BranchInfo
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
  val writeInfo     = new BufferInfo
  val dcachehitVec  = Vec(WAY_WIDTH, Bool())
  val ldData        = UInt(DATA_WIDTH.W)
  val wdata         = UInt(DATA_WIDTH.W)
  val wmask         = UInt((DATA_WIDTH / 8).W)
  val rollback      = Bool()
  val forwardHitVec = Vec(2, Bool())
  val forwardData   = Vec(2, UInt(DATA_WIDTH.W))
  val forwardStrb   = Vec(2, UInt((DATA_WIDTH / 8).W))

  // storebuffer
  val storeBufferHit     = Bool()
  val storeBufferHitData = UInt(DATA_WIDTH.W)
  val storeBufferHitStrb = UInt((DATA_WIDTH / 8).W)

  override def getFlushInfo: BasicStageInfo = {
    val info = WireDefault(0.U.asTypeOf(new SingleInfo))
    info.bubble := true.B
    info
  }
}

class DualInfo extends BasicStageInfo {
  val bits = Vec(ISSUE_WIDTH, new SingleInfo)

  override def getFlushInfo: BasicStageInfo = {
    val info = WireDefault(0.U.asTypeOf(new DualInfo))
    for (i <- 0 until ISSUE_WIDTH) {
      info.bits(i).bubble := true.B
    }
    info
  }
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
