package isa

import chisel3._
import chisel3.util._

object BruOptype {
  def b  = "b1000".U
  def bl = "b1001".U

  def beq  = "b0000".U
  def bne  = "b0001".U
  def blt  = "b0010".U
  def bge  = "b0011".U
  def bltu = "b0100".U
  def bgeu = "b0101".U
  def jirl = "b0110".U

  def isimm26(bruOpType: UInt): Bool = bruOpType(3).asBool

  def apply() = UInt(4.W)
}

object AluOpType {
  def add  = "b00000".U
  def sub  = "b00001".U
  def slt  = "b00010".U
  def sltu = "b00011".U
  def nor  = "b00100".U

  def and = "b10000".U
  def or  = "b10001".U
  def xor = "b10010".U

  def sll = "b01000".U
  def srl = "b01001".U
  def sra = "b01010".U

  def idle = "b11111".U

  def isimm5(aluOpType: UInt): Bool = aluOpType(3)
  def isimmu(aluOpType: UInt): Bool = aluOpType(4)

  def apply() = UInt(5.W)
}

object DivOpType {
  def umod = "b00".U
  def u    = "b10".U
  def smod = "b01".U
  def s    = "b11".U

  def signed(divOpType: UInt): Bool = divOpType(0).asBool

  def apply() = UInt(2.W)
}

object MulOpType {
  def slow    = "b00".U
  def ulow    = "b01".U
  def shigh   = "b10".U
  def uhigh   = "b11".U
  def apply() = UInt(2.W)
}

object MemOpType {
  def readw  = "b1100".U
  def readh  = "b1011".U
  def readhu = "b1010".U
  def readb  = "b1001".U
  def readbu = "b1000".U
  def ll     = "b1101".U

  def writew = "b0000".U
  def writeh = "b0001".U
  def writeb = "b0010".U
  def sc     = "b0011".U

  def cacop = "b0100".U
  def ibar  = "b0101".U

  def isread(memOpType: UInt): Bool  = memOpType(3)
  def iswrite(memOpType: UInt): Bool = !memOpType(3) && !memOpType(2)
  def ish(memOpType: UInt): Bool     = memOpType(2, 1) === "b01".U
  def isb(memOpType: UInt): Bool     = memOpType(2, 1) === "b00".U
  def isatom(memOpType: UInt): Bool  = memOpType === ll || memOpType === sc // can be improved?
  def signed(memOpType: UInt): Bool  = memOpType(0).asBool
  def apply()                        = UInt(4.W)
}

object CsrOpType {
  def rd      = "b00".U
  def wr      = "b01".U
  def xchg    = "b10".U
  def cntrd   = "b11".U
  def apply() = UInt(2.W)
}

object CntOpType {
  def cnth    = "b00".U
  def cntl    = "b01".U
  def apply() = UInt(2.W)
}

object ExcOpType {
  def sys     = "b00".U
  def ertn    = "b01".U
  def brk     = "b10".U
  def apply() = UInt(2.W)
}

object TlbOpType {
  def rd      = "b000".U
  def wr      = "b001".U
  def srch    = "b010".U
  def fill    = "b011".U
  def inv     = "b100".U
  def apply() = UInt(3.W)
}
