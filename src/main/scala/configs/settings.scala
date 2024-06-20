package configs

import chisel3._
import chisel3.util._

object CpuConfig {
  val hasBlackBox: Boolean   = true
  val debug_on: Boolean      = true
  val statistics_on: Boolean = true
  val divClockNum: Int       = 8
  val mulClockNum: Int       = 2
}
