package bundles

import chisel3._
import chisel3.util._

import const.Parameters._
import const.memType
import const.ECodes
import isa.TlbOpType
import const.tlbConst._
import bundles._
import const.Config

class CSRTLBIO extends Bundle {
  val is_direct = Output(Bool())
  val asid      = Output(new ASID_info)
  val crmd      = Output(new CRMD_info)
  val dmw       = Output(Vec(2, new DMW_info))
  val tlbehi    = Output(new TLBEHI_info)
  val estat     = Output(new ESTAT_info)
  val tlbelo0   = Output(new TLBELO_info)
  val tlbelo1   = Output(new TLBELO_info)
  val tlbidx    = Output(new TLBIDX_info)

  val tlbwe    = Input(Bool())
  val opType   = Input(TlbOpType())
  val tlbe     = Input(new TLBEntry)
  val hitted   = Input(Bool())
  val hitIndex = Input(UInt(TLB_INDEX_LEN.W))
}

// VIPT形式
class Stage0TLBIO extends Bundle {
  val va       = Output(UInt(ADDR_WIDTH.W))
  val memType  = Output(const.memType())
  val unitType = Output(Bool()) // 0: fetch    1: load/store

  val hitVec = Input(Vec(TLB_ENTRIES, Bool()))
}

class Stage1TLBIO extends Bundle {
  val va     = Output(UInt(ADDR_WIDTH.W))
  val hitVec = Output(Vec(TLB_ENTRIES, Bool()))

  val pa        = Input(UInt(ADDR_WIDTH.W))
  val cached    = Input(Bool())
  val exception = Input(new ExcInfo)

  val tlb_refill_index = if (Config.debug_on_chiplab) Some(Input(UInt(TLB_INDEX_LEN.W))) else None
}

class TlbBufferInfo extends Bundle {
  val en = Bool()                // isTLBInst
  val op = UInt(INV_OP_LENGTH.W) // rd.areg
  val inv = new Bundle {
    val asid = UInt(10.W)         // rj.data(9:0)
    val va   = UInt(ADDR_WIDTH.W) // rk.data
  }
  val opType = TlbOpType() // tlb inst optype
}
