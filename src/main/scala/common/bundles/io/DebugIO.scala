package bundles

import chisel3._
import chisel3.util._

class DifftestInstrCommit extends Bundle {
  // val clock          = Clock()
  val coreid         = UInt(8.W)
  val index          = UInt(8.W)
  val valid          = Bool()
  val pc             = UInt(64.W)
  val instr          = UInt(32.W)
  val skip           = Bool()
  val is_TLBFILL     = Bool()
  val TLBFILL_index  = UInt(5.W)
  val is_CNTinst     = Bool()
  val timer_64_value = UInt(64.W)
  val wen            = Bool()
  val wdest          = UInt(8.W)
  val wdata          = UInt(64.W)
  val csr_rstat      = Bool()
  val csr_data       = UInt(32.W)
}

class DifftestExcpEvent extends Bundle {
  // val clock         = Clock()
  val coreid        = UInt(8.W)
  val excp_valid    = Bool()
  val eret          = Bool()
  val intrNo        = UInt(32.W)
  val cause         = UInt(32.W)
  val exceptionPC   = UInt(64.W)
  val exceptionInst = UInt(32.W)
}

class DifftestTrapEvent extends Bundle { //
  // val clock    = Clock()
  val coreid   = UInt(8.W)
  val valid    = Bool() // 0
  val code     = UInt(3.W)
  val pc       = UInt(64.W)
  val cycleCnt = UInt(64.W)
  val instrCnt = UInt(64.W)
}

class DifftestStoreEvent extends Bundle {
  // val clock      = Clock()
  val coreid     = UInt(8.W)
  val index      = UInt(8.W)
  val valid      = UInt(8.W)
  val storePAddr = UInt(64.W)
  val storeVAddr = UInt(64.W)
  val storeData  = UInt(64.W)
}

class DifftestLoadEvent extends Bundle {
  // val clock  = Clock()
  val coreid = UInt(8.W)
  val index  = UInt(8.W)
  val valid  = UInt(8.W)
  val paddr  = UInt(64.W)
  val vaddr  = UInt(64.W)
}

class DifftestCSRRegState extends Bundle {
  // val clock     = Clock()
  val coreid    = UInt(8.W)
  val crmd      = UInt(32.W)
  val prmd      = UInt(32.W)
  val euen      = UInt(32.W)
  val ecfg      = UInt(32.W)
  val estat     = UInt(32.W)
  val era       = UInt(32.W)
  val badv      = UInt(32.W)
  val eentry    = UInt(32.W)
  val tlbidx    = UInt(32.W)
  val tlbehi    = UInt(32.W)
  val tlbelo0   = UInt(32.W)
  val tlbelo1   = UInt(32.W)
  val asid      = UInt(32.W)
  val pgdl      = UInt(32.W)
  val pgdh      = UInt(32.W)
  val save0     = UInt(32.W)
  val save1     = UInt(32.W)
  val save2     = UInt(32.W)
  val save3     = UInt(32.W)
  val tid       = UInt(32.W)
  val tcfg      = UInt(32.W)
  val tval      = UInt(32.W)
  val ticlr     = UInt(32.W)
  val llbctl    = UInt(32.W)
  val tlbrentry = UInt(32.W)
  val dmw0      = UInt(32.W)
  val dmw1      = UInt(32.W)
}

class DifftestGRegState extends Bundle {
  // val clock  = Clock()
  val coreid = UInt(8.W)
  val gpr    = Vec(32, UInt(32.W))
}

class RobCommitBundle extends Bundle {
  val DifftestInstrCommit = new DifftestInstrCommit
  val DifftestExcpEvent   = new DifftestExcpEvent
  val DifftestTrapEvent   = new DifftestTrapEvent
  val DifftestStoreEvent  = new DifftestStoreEvent
  val DifftestLoadEvent   = new DifftestLoadEvent
}

class CommitBundle extends Bundle {
  val DifftestInstrCommit = new DifftestInstrCommit
  val DifftestExcpEvent   = new DifftestExcpEvent
  val DifftestTrapEvent   = new DifftestTrapEvent
  val DifftestStoreEvent  = new DifftestStoreEvent
  val DifftestLoadEvent   = new DifftestLoadEvent
  val DifftestCSRRegState = new DifftestCSRRegState
  val DifftestGRegState   = new DifftestGRegState
}
