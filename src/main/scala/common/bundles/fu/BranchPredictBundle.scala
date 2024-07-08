package bundles

import chisel3._
import chisel3.util._

class br extends Bundle {
  val en  = Bool()
  val tar = UInt(32.W)
}
