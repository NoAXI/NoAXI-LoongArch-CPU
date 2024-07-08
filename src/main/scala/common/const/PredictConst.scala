package const

import chisel3._
import chisel3.util._

import const.Parameters._

// BTB
// ============================================
//   54   |53       34|33           2|1      0|
// valid  |    tag    | branchTarget |  type  |
// ============================================

object Predict {
  val INDEX_LENGTH   = 6
  val INDEX_WIDTH    = 1 << INDEX_LENGTH
  val HISTORY_LENGTH = 3
  val HISTORY_WIDTH  = 1 << HISTORY_LENGTH
  val COUNTER_LENGTH = 2
  val COUNTER_WIDTH  = 1 << COUNTER_LENGTH

  // BTB
  val BTB_INDEX_LENGTH = 10 // maybe 7
  val BTB_INDEX_WIDTH  = 1 << BTB_INDEX_LENGTH

  val BTB_TAG_LENGTH  = ADDR_WIDTH - BTB_INDEX_LENGTH - 2         // 20
  val BTB_FLAG_LENGTH = 2                                         // isCALL and isReturn
  val BTB_INFO_LENGTH = BTB_TAG_LENGTH + 32 + BTB_FLAG_LENGTH + 1 // 53

  // RAS
  val RAS_DEPTH = 8
  val RAS_WIDTH = log2Ceil(RAS_DEPTH)
}
