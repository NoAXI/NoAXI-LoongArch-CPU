package const

import chisel3._
import chisel3.util._

object Parameters {
  // data width
  val DATA_WIDTH = 32
  val INST_WIDTH = 32
  val ADDR_WIDTH = 32
  val CSR_WIDTH  = 14

  // fetch width
  val FETCH_DEPTH = 2 // maybe 4

  // decoded info width
  val FUNC_TYPE_WIDTH     = 4
  val OP_TYPE_WIDTH       = 5
  val PIPELINE_TYPE_WIDTH = 2

  // reg rename width
  val AREG_WIDTH     = 5
  val PREG_WIDTH     = 6
  val FREELIST_WIDTH = 5
  val ROB_WIDTH      = 4
  val OPERAND_MAX    = 2
  val ROB_NUM        = 1 << ROB_WIDTH
  val AREG_NUM       = 1 << AREG_WIDTH
  val PREG_NUM       = 1 << PREG_WIDTH
  val FREELIST_NUM   = 1 << FREELIST_WIDTH

  // issue width
  val ISSUE_WIDTH       = 2
  val BACK_ISSUE_WIDTH  = 4
  val ARITH_ISSUE_NUM   = 2                   // real id = 0, 1
  val MULDIV_ISSUE_ID   = ARITH_ISSUE_NUM     // real id = 2
  val MEMORY_ISSUE_ID   = ARITH_ISSUE_NUM + 1 // real id = 3
  val AWAKE_NUM         = 4
  val FORWARD_STAGE_NUM = 2

  // backend stage const
  val ARITH_STAGE_NUM  = 2 // readreg, arith
  val MULDIV_STAGE_NUM = 2 // readreg, muldiv
  val MEMORY_STAGE_NUM = 4 // readreg, mem0, mem1, mem2

  // write buffer const
  val STORE_BUFFER_WIDTH       = 2
  val STORE_BUFFER_LENGTH      = 1 << STORE_BUFFER_WIDTH
  val WRITE_BACK_BUFFER_WIDTH  = 2
  val WRITE_BACK_BUFFER_LENGTH = 1 << WRITE_BACK_BUFFER_WIDTH

  // issue queue width
  val ARITH_QUEUE_WIDTH  = 2
  val MULDIV_QUEUE_WIDTH = 2
  val MEMORY_QUEUE_WIDTH = 3
  val ARITH_QUEUE_SIZE   = 1 << ARITH_QUEUE_WIDTH
  val MULDIV_QUEUE_SIZE  = 1 << MULDIV_QUEUE_WIDTH
  val MEMORY_QUEUE_SIZE  = 1 << MEMORY_QUEUE_WIDTH

  // others
  val START_ADDR = 0x1bfffffc
  val ALL_MASK   = "b1111_1111_1111_1111_1111_1111_1111_1111"
  val COUNT_N    = 28
}
