package const

import chisel3._
import chisel3.util._

object Parameters {
  // data width
  val DATA_WIDTH = 32
  val INST_WIDTH = 32
  val ADDR_WIDTH = 32
  val CSR_WIDTH  = 14

  // decoded info width
  val OP_TYPE_WIDTH       = 5
  val PIPELINE_TYPE_WIDTH = 2

  // reg rename width
  val AREG_WIDTH     = 5
  val PREG_WIDTH     = 6
  val FREELIST_WIDTH = 5
  val ROB_WIDTH      = 5
  val ROB_NUM        = 32
  val OPERAND_MAX    = 2
  val AREG_NUM       = 1 << AREG_WIDTH
  val PREG_NUM       = 1 << PREG_WIDTH
  val FREELIST_NUM   = 1 << FREELIST_WIDTH

  // issue width
  val ISSUE_WIDTH      = 2
  val BACK_ISSUE_WIDTH = 4
  val ARITH_ISSUE_NUM  = 2

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
