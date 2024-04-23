package stages

import chisel3._
import chisel3.util._

class br_bus extends Bundle {
    val br_taken = Bool()
    val br_target = UInt(32.W)
}
