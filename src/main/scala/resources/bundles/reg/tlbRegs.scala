package bundles

import chisel3._
import const.Parameters._

// TLBCompare
// ==================================================================================================================
//  36                       18|17                 12|11            11|10                             1|0        0|
//   VPPN(Virtual Page Number) |    PS(Page Size)    | G(Global sign) | ASID(Address Space Identifier) | E(Exist) |
// ==================================================================================================================

// TLBTransform
// ============================================================================================================
//  25                        6|5                     4|3                             2|1        1|0        0|
//   PPN(Physical Page Number) |  PLV(Privilege Level) | MAT(Mapping Assistance Table) | D(Dirty) | V(Valid) |
// ============================================================================================================

class TLBEntry extends Bundle {
  val vppn = UInt(19.W)         // Virtual Page Number
  val ps   = UInt(6.W)          // Page Size
  val g    = Bool()             // Global sign
  val asid = UInt(10.W)         // Address Space Identifier
  val e    = Bool()             // Exist
  val ppn  = Vec(2, UInt(20.W)) // Physical Page Number
  val plv  = Vec(2, UInt(2.W))  // Privilege Level
  val mat  = Vec(2, UInt(2.W))  // Mapping Assistance Table
  val d    = Vec(2, Bool())     // Dirty
  val v    = Vec(2, Bool())     // Valid
}

class TLBTransform extends Bundle {
  val ppn = UInt(20.W)
  val plv = UInt(2.W)
  val mat = UInt(2.W) // Mapping Assistance Table
  val d   = Bool()
  val v   = Bool()
}
