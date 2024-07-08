package bundles

import chisel3._
import chisel3.util._

import const.Parameters._
import const.memType
import const.ECodes
import isa.TlbOpType
import const.tlbConst._
import bundles._

class CSRTLBIO extends Bundle {
  val is_direct = Output(Bool())
  val asid      = Output(new ASID_info)
  val crmd      = Output(new CRMD_info)
  val dmw       = Output(Vec(2, new DMW_info))
  // val tlbehi    = Output(new TLBEHI_info)
}

// VIPT形式
class Stage0TLBIO extends Bundle {
  val va      = Output(UInt(ADDR_WIDTH.W))
  val memType = Output(const.memType())

  val hitVec   = Input(Vec(TLB_ENTRIES, Bool()))
  val isDirect = Input(Bool())
  val directpa = Input(UInt(ADDR_WIDTH.W))
}

class Stage1TLBIO extends Bundle {
  val va     = Output(UInt(ADDR_WIDTH.W))
  val hitVec = Output(Vec(TLB_ENTRIES, Bool()))

  val pa        = Input(UInt(ADDR_WIDTH.W))
  val cached    = Input(Bool())
  val exception = Input(new Exception)
}
