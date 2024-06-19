package pipeline.backend

import chisel3._
import chisel3.util._
import const.Parameters._

class ROB_entry extends Bundle {
  val pc    = UInt(ADDR_WIDTH.W)
  val areg  = UInt(REG_WIDTH.W)
  val preg  = UInt(PREG_WIDTH.W)
  val opreg = UInt(PREG_WIDTH.W)
}
class ROB_table extends Bundle {
}

class ROB extends Module {
  val io = new Bundle {
    val aROB = Output(new ROB_table)
  }
  
}
