package bundles

import chisel3._
import chisel3.util._

class br extends Bundle {
  val en     = Output(Bool())
  val exc_en = Output(Bool())
  val tar    = Output(UInt(32.W))
}
