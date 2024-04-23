package config

import chisel3._
import chisel3.util._

import math._
import isa._

class info extends Bundle {
  val pc   = UInt(32.W)
  val inst = UInt(32.W)

  val func_type = FuncType()
  val op_type   = UInt(10.W)
  val is_wf     = Bool()

  val dest      = UInt(5.W)
  val rkd_value = UInt(32.W)

  val src1 = UInt(32.W)
  val src2 = UInt(32.W)
  val alu_result   = UInt(32.W)
}

object Functions {
  // 前一个阶段和这个阶段握手，并获得数据
  def ConnectGetBus(x: DecoupledIO[info], y: DecoupledIO[info]): info = {
    val info     = RegInit(0.U.asTypeOf(new info))
    val valid    = RegInit(false.B)
    val ready_go = true.B
    x.ready := !valid || ready_go && y.ready
    y.valid := valid && ready_go
    when(x.ready) {
      valid := x.valid
    }
    when(x.valid && x.ready) {
      info := x.bits
    }
    info
  }

  def SignedExtend(a: UInt, len: Int) = {
      val aLen = a.getWidth
      val signBit = a(aLen-1)
      if (aLen >= len) a(len-1,0) else Cat(Fill(len - aLen, signBit), a)
  }

  def UnSignedExtend(a: UInt, len: Int) = {
      val aLen = a.getWidth
      if (aLen >= len) a(len-1,0) else Cat(0.U((len - aLen).W), a)
  }

  def lookup[T <: Data](key: UInt, default: T, map: Array[(BitPat, T)]): T = {
    val result = WireDefault(default)
    for ((pattern, value) <- map) {
      when(key === pattern) {
        result := value
      }
    }
    result
  }

  // 没怎么看懂
  def MateDefault[T <: Data](key: UInt, default: T, map: Iterable[(UInt, T)]): T =
    MuxLookup(key, default)(map.toSeq)
}

trait Parameters {
  val DATA_WIDTH_D = 64               // 双字
  val DATA_WIDTH_W = DATA_WIDTH_D / 2 // 字
  val DATA_WIDTH_H = DATA_WIDTH_W / 2 // 半字
  val DATA_WIDTH_B = DATA_WIDTH_H / 2 // 字节
  // val DATA_WIDTH_b = 1 // 位

  val INST_WIDTH   = 32     // 指令长度
  val INST_WIDTH_B = 32 / 8 // 指令字节长度

  val GR_SIZE = 32 // 通用寄存器数量
  val GR_LEN  = 64 // 通用寄存器长度(位宽)

  val START_ADDR = 0x1bfffffc // 程序起始地址
  val ADDR_WIDTH = 32         // 通用寄存器地址长度(not sure, maybe 是 max)

  val LS_TYPE_WIDTH = 3 // Load/Store 类型长度
}

