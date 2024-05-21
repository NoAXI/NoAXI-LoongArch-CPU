package const

import chisel3._
import chisel3.util._

object Parameters {
  val DATA_WIDTH = 32
  val INST_WIDTH = 32
  val ADDR_WIDTH = 32
  val CSR_WIDTH  = 14
  val REG_WIDTH  = 5
  val START_ADDR = 0x1bfffffc
  val ALL_MASK   = "b1111_1111_1111_1111_1111_1111_1111_1111"
  val COUNT_N    = 28
}