package bundles

import chisel3._
import chisel3.util._
import const.Parameters._
import const.Predict._

class BranchInfo extends Bundle {
  val en  = Bool()
  val tar = UInt(ADDR_WIDTH.W)
}

class BranchCheck extends Bundle {
  val en      = Bool()
  val succeed = Bool()
  val real    = Bool()
  val index   = UInt(INDEX_LENGTH.W)
}
