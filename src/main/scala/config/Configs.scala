package config

import chisel3._
import chisel3.util._

import math._
import isa._

class info extends Bundle with Parameters {
  val pc   = UInt(ADDR_WIDTH.W)
  val inst = UInt(INST_WIDTH.W)

  val func_type = FuncType()
  val op_type   = UInt(10.W)
  val is_wf     = Bool()
  val piece     = UInt(2.W)

  val dest      = UInt(ADDR_WIDTH_REG.W)
  val rkd_value = UInt(DATA_WIDTH.W)

  val src1   = UInt(DATA_WIDTH.W)
  val src2   = UInt(DATA_WIDTH.W)
  val result = UInt(DATA_WIDTH.W)

  val this_exc = Bool() // 普通例外
  val csr_we   = Bool()
  val csr_addr = UInt(14.W)
  val csr_val  = UInt(DATA_WIDTH.W)
  val ecode    = UInt(15.W)
  val csr_mask = UInt(DATA_WIDTH.W)
}

class pair[A, B](val first: A, val second: B)

object Functions {
  // 前一个阶段和这个阶段握手，并获得数据
  def ConnectGetBus(x: DecoupledIO[info], y: DecoupledIO[info]): info = {
    val info  = RegInit(0.U.asTypeOf(new info))
    val valid = RegInit(false.B)
    // val ready_go = true.B
    // x.ready := !valid || ready_go && y.ready
    x.ready := !valid || y.ready
    y.valid := valid
    when(x.ready) {
      valid := x.valid
    }
    when(x.valid && x.ready) {
      info := x.bits
    }
    info
  }

  def SignedExtend(a: UInt, len: Int) = {
    val aLen    = a.getWidth
    val signBit = a(aLen - 1)
    if (aLen >= len) a(len - 1, 0) else Cat(Fill(len - aLen, signBit), a)
  }

  def UnSignedExtend(a: UInt, len: Int) = {
    val aLen = a.getWidth
    if (aLen >= len) a(len - 1, 0) else Cat(0.U((len - aLen).W), a)
  }

  def Extend(a: UInt, len: Int, typ: UInt) = {
    Mux(typ === SrcType.immu, UnSignedExtend(a, len), SignedExtend(a, len))
  }

  def Extend(a: UInt, len: Int, typ: Bool) = {
    Mux(typ, SignedExtend(a, len), UnSignedExtend(a, len))
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

  def writeMask(mask: UInt, data: UInt, wdata: UInt): UInt = {
    val result = VecInit((0.U(32.W)).asBools)
    for (i <- 0 until 32) {
      result(i) := Mux(mask(i), wdata(i), data(i))
    }
    result.asUInt
  }

  // 没怎么看懂
  def MateDefault[T <: Data](key: UInt, default: T, map: Iterable[(UInt, T)]): T =
    MuxLookup(key, default)(map.toSeq)
}

trait Parameters {
  val DATA_WIDTH   = 32
  val DATA_WIDTH_B = 32 / 8

  val INST_WIDTH   = 32     // 指令长度
  val INST_WIDTH_B = 32 / 8 // 指令字节长度

  val GR_SIZE = 32 // 通用寄存器数量
  val GR_LEN  = 64 // 通用寄存器长度(位宽)

  val START_ADDR     = 0x1bfffffc // 程序起始地址
  val ADDR_WIDTH     = 32         // 通用寄存器地址长度(not sure, maybe 是 max)
  val ADDR_WIDTH_REG = 5

  val LS_TYPE_WIDTH = 3 // Load/Store 类型长度

  val ALL_MASK = "b1111_1111_1111_1111_1111_1111_1111_1111"

  val COUNT_N = 28
}
