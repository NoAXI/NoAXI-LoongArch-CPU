package const

import chisel3.util._

// ==============================================
//  addr      |31       12|11         4|3      0|
//            |    tag    | line_index | offset |
// ==============================================

object cacheConst {
  val PAGE_SIZE = 4096 // page's size is 4KB
  val LINE_SIZE = 16   // line's size is 16B

  val LINE_WIDTH     = PAGE_SIZE / LINE_SIZE // 256 lines
  val LINE_WIDTH_LOG = log2Ceil(LINE_WIDTH)  // 8 bits

  val BANK_WIDTH_LOG = log2Ceil(LINE_SIZE) // 4 bits
  val BANK_WIDTH     = 1 << BANK_WIDTH_LOG // 16 Bytes

  val WAY_WIDTH = 2
  val TAG_WIDTH = 32 - LINE_WIDTH_LOG - BANK_WIDTH_LOG // 32 - 8 - 4 = 20 bits
}

// way0
//
// =======================================================
//  Line0(16B) tag  |bank 0 | bank 1  |  bank 2 | bank 3 |
//                  | 32    |   32    |   32    |  32    |
// =======================================================
// =======================================================
//  Line1      tag  |bank 0 | bank 1  |  bank 2 | bank 3 |
//                  | 32    |   32    |   32    |  32    |
// =======================================================
// =======================================================
//  Line2      tag  |bank 0 | bank 1  |  bank 2 | bank 3 |
//                  | 32    |   32    |   32    |  32    |
// =======================================================
// ...
// =======================================================
//  Line2^8-1  tag  |bank 0 | bank 1  |  bank 2 | bank 3 |
//                  | 32    |   32    |   32    |  32    |
// =======================================================

