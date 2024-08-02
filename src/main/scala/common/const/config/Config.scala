package const

import chisel3._
import chisel3.util._

object Config {
  val isOnChip: Boolean      = true
  val hasBlackBox: Boolean   = false | isOnChip
  val debug_on: Boolean      = true & !isOnChip
  val statistic_on: Boolean  = true & !isOnChip
  val staticPredict: Boolean = false
  val loadSpecial: Boolean   = isOnChip
  val divClockNum: Int       = 8
  val mulClockNum: Int       = 2
}
