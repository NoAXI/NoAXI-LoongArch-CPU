package bundles

import chisel3._
import chisel3.util._

import const.Parameters._

object ROB_const {
  val ROB_NUM = 32
}

class ROB_info extends Bundle {
  val pc    = UInt(ADDR_WIDTH.W)
  val areg  = UInt(REG_WIDTH.W)
  val preg  = UInt(PREG_WIDTH.W)
  val opreg = UInt(PREG_WIDTH.W)
}

class ROB_reg(N: Int = ROB_const.ROB_NUM) extends Bundle {
  val reg = RegInit(VecInit(Seq.fill(N)(0.U.asTypeOf(new ROB_info))))

  def write(index: UInt, data: ROB_info): Unit = {
    val a = 1
  }
  def read(index: UInt): ROB_info = {
    reg(index).asTypeOf(new ROB_info)
  }

}
