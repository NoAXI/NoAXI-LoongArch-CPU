package stages

import chisel3._
import chisel3.util._

class rf_bus extends Bundle {
  val we    = Bool()
  val waddr = UInt(5.W)
  val wdata = UInt(32.W)
}

class br_bus extends Bundle {
  val br_taken  = Bool()
  val br_target = UInt(32.W)
}
