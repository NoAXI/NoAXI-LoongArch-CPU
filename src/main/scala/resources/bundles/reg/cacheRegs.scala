package bundles

import chisel3._
import chisel3.util._

import const.cacheConst._

class Way extends Bundle {
    val valid = Bool()
    val dirty = Bool()
    val tag   = UInt(32.W)
    val data  = Vec(BANK_WIDTH, UInt(32.W))
}