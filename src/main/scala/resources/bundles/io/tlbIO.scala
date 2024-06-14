package bundles

import chisel3._
import chisel3.util._

import const.Parameters._
import const.memType

class csr_TLB_IO extends Bundle {
  val is_direct = Output(Bool())
  val asid      = Output(new ASID_info)
  val crmd      = Output(new CRMD_info)
  val dmw       = Output(Vec(2, new DMW_info))
}

class mem_TLB_IO extends Bundle {
  val request  = Output(Bool())
  val va       = Output(UInt(ADDR_WIDTH.W))
  val mem_type = Output(memType())
}
