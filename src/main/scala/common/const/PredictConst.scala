package const

import chisel3._
import chisel3.util._

import const.Parameters._

// BTB
// ============================================
//   54   |53       34|33           2|1      0|
// valid  |    tag    | branchTarget |  type  |
// ============================================

// type
// ======================
// |1                0|
// |isCALL   isReturn |
// ======================

// pay attention!!!: HISTORY_LENGTH infects the code length strongly (also time order)!!! because of PHT is reg!!!
// 10 10 10 ipc = 0.794962 90hz failed
// 7 7 7 95hz failed
// 10 6 7 ipc = 0.761868 95hz failed
// 10 5 7 ipc = 0.753462
// 10 5 10 ipc = 0.757329
// 10 3 10 ipc = 0.747394 92hz failed 90hz passed
// 10 5 10 ipc = 0.757329 90hz passed
// 10 7 10 ipc = 0.770573 90hz failed
object Predict {
  val INDEX_LENGTH   = 10
  val INDEX_WIDTH    = 1 << INDEX_LENGTH
  val HISTORY_LENGTH = 7
  val HISTORY_WIDTH  = 1 << HISTORY_LENGTH
  val COUNTER_LENGTH = 2
  val COUNTER_WIDTH  = 1 << COUNTER_LENGTH

  // BTB
  val BTB_INDEX_LENGTH = 10 // be equal to INDEX_WIDTH because of BHR should be the same length
  val BTB_INDEX_WIDTH  = 1 << BTB_INDEX_LENGTH

  val BTB_TAG_LENGTH  = ADDR_WIDTH - BTB_INDEX_LENGTH - 2                 // 20
  val BTB_FLAG_LENGTH = 2                                                 // isCALL and isReturn
  val BTB_INFO_LENGTH = 1 + BTB_TAG_LENGTH + ADDR_WIDTH + BTB_FLAG_LENGTH // 55

  // RAS
  val RAS_DEPTH = 8
  val RAS_WIDTH = log2Ceil(RAS_DEPTH)
}
