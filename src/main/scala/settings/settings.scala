package settings

import chisel3._
import chisel3.util._

object CpuConfig {
    val hasBlackBox: Boolean = false
    val divClockNum: Int = 8
    val mulClockNum: Int = 2
}