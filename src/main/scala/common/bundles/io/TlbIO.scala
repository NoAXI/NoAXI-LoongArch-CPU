package bundles

import chisel3._
import chisel3.util._

import const.Parameters._
import const.memType
import const.ECodes
import isa.TlbOpType

class CSRTLBIO extends Bundle {
  val is_direct = Output(Bool())
  val asid      = Output(new ASID_info)
  val crmd      = Output(new CRMD_info)
  val dmw       = Output(Vec(2, new DMW_info))
  // val tlbehi    = Output(new TLBEHI_info)
}

class Mem0TLBIO extends Bundle {
  val va        = Output(UInt(ADDR_WIDTH.W))
  // these are used to judge the exception
  val mem_type  = Output(memType()) 
  val exc_type  = Input(ECodes()) 
  val exc_vaddr = Input(UInt(ADDR_WIDTH.W))
  
  val pa        = Input(UInt(ADDR_WIDTH.W))
  val cached    = Input(Bool())
}

// VIPT形式
class PreFetchTLBIO extends Bundle {
  val va = Output(UInt(ADDR_WIDTH.W))
}

class FetchTLBIO extends Bundle {
  val pa     = Input(UInt(ADDR_WIDTH.W))
  // these are used to judge the exception
  // val mem_type  = Output(memType()) 
  // val exc_type  = Input(ECodes()) 
  // val exc_vaddr = Input(UInt(ADDR_WIDTH.W))
  val cached = Input(Bool())
}
