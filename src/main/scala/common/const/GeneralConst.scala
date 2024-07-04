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
  val OP_TYPE_WIDTH = 5

  // reg rename width
  val AREG_WIDTH     = 5
  val AREG_NUM       = 1 << AREG_WIDTH
  val PREG_WIDTH     = 6
  val PREG_NUM       = 1 << PREG_WIDTH
  val FREELIST_WIDTH = 5
  val FREELIST_NUM   = 1 << FREELIST_WIDTH
  val ROB_WIDTH      = 5
  val ROB_NUM        = 32
  val OPERAND_MAX    = 2

  // issue width
  val ISSUE_WIDTH      = 2
  val BACK_ISSUE_WIDTH = 4

  // others
  val START_ADDR = 0x1bfffffc
  val ALL_MASK   = "b1111_1111_1111_1111_1111_1111_1111_1111"
  val COUNT_N    = 28
}
