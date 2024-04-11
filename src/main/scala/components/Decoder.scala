package components

import chisel3._
import chisel3.util._

import config._
import isa.LA64_ALUInst

// 有rd的就是要写入寄存器吧
// 目前是乱编码的
trait InstType {  // 指令类型
    def Inst2R      = "b0010".U
    def Inst3R      = "b0011".U
    def Inst4R      = "b0100".U

    def Inst2RI8    = "b0000".U
    def Inst2RI12   = "b0001".U
    def Inst2RI14   = "b0101".U
    def Inst2RI16   = "b0110".U

    def Inst1RI21   = "b1000".U
    def InstI26     = "b1001".U

    def isWriteReg(instType: UInt): Bool = !instType(3)  // 是否写寄存器
    def apply() = UInt(4.W) 
}

object SrcType {  // 源操作数类型
  def reg = "b0".U
  def pc  = "b1".U
  def imm = "b1".U
  def apply() = UInt(1.W)
}

object FuncType {  //功能类型
    def alu = "b0".U
    def apply() = UInt(1.W)
}

// object FuncOpType {
//     def add = "b000".U
//     def sub = "b001".U
//     def apply() = UInt(3.W)
// }

object Instructions extends InstType with Parameters {
    // def NOP = ANDI r0 r0 0
    def DecodeTable = LA64_ALUInst.table
}