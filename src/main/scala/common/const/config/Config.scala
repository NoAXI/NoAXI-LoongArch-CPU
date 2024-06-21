package const

import chisel3._
import chisel3.util._

object Config {
  val hasBlackBox: Boolean  = false
  val debug_on: Boolean     = true
  val statistic_on: Boolean = true
  val divClockNum: Int      = 8
  val mulClockNum: Int      = 2
  val fetch_depth: Int      = 4 // 取指深度：4
}
