package const

import chisel3._
import chisel3.util._

object Config {
  val isOnChip: Boolean         = false
  val debug_on_chiplab: Boolean = false
  val hasBlackBox: Boolean      = false | isOnChip
  val debug_on: Boolean         = true & !isOnChip
  val statistic_on: Boolean     = true & !isOnChip
  val staticPredict: Boolean    = false
  val ext_int_on: Boolean       = true
  val loadSpecial: Boolean      = isOnChip
  val divClockNum: Int          = 8
  val mulClockNum: Int          = 2
}
