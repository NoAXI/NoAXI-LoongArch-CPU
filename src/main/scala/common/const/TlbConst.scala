package const

import chisel3._
import chisel3.util._

object memType {
  val fetch   = 3.U(2.W)
  val load    = 1.U(2.W)
  val store   = 2.U(2.W)
  def apply() = UInt(2.W)
}

object tlbConst {
  val MEM_TYPE_SIZE = 2
  val TLB_ENTRIES   = if(Config.debug_on_chiplab) 32 else 16
  val TLB_INDEX_LEN = log2Ceil(TLB_ENTRIES)
  val INV_OP_LENGTH = 5
}

/*
mat
0    -- 强序非缓存
1    -- 一致可缓
2/3  -- 保留
 */
