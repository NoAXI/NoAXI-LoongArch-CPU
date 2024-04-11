error id: file://<WORKSPACE>/src/main/scala/config/Configs.scala:[2259..2259) in Input.VirtualFile("file://<WORKSPACE>/src/main/scala/config/Configs.scala", "package config

import chisel3._
import chisel3.util._

import math._

object Functions {
    def SignedExtend(x: UInt, len: Int): UInt = {
        val len_x = x.getWidth
        if (len_x == len) {
            x
        } else {
            val fill = Fill(len - len_x, x(len_x - 1))
            Cat(fill, x)
        }
    }

    def UnSignedExtend(x: UInt, len: Int): UInt = {
        val len_x = x.getWidth
        if (len_x == len) {
            x
        } else {
            val fill = Fill(len - len_x, 0.U)
            Cat(fill, x)
        }
    }
}

trait Parameters {
    val DATA_WIDTH_D = 64  // 双字
    val DATA_WIDTH_W = DATA_WIDTH_D / 2  // 字
    val DATA_WIDTH_H = DATA_WIDTH_W / 2  // 半字
    val DATA_WIDTH_B = DATA_WIDTH_H / 2  // 字节
    val DATA_WIDTH_b = 1 // 位

    val INST_WIDTH = 32  // 指令长度

    val GR_LEN = 64  // 通用寄存器长度
    
    val ADDR_WIDTH = 32 // 通用寄存器地址长度(not sure, maybe 是 max)

    val LS_TYPE_WIDTH = 3  // Load/Store 类型长度
}

// object Instructions {
//     def ADD_W       = BitPat("b00000000000100000????????????")
//     def ADD_D       = BitPat("b00000000000100001????????????")
//     def SUB_W       = BitPat("b00000000000100010????????????")
//     def SUB_D       = BitPat("b00000000000100011????????????")
//     def ADDI_W      = BitPat("b0000001010???????????????????")
//     def ADDI_D      = BitPat("b0000001011???????????????????")
//     def ADDU16I_D   = BitPat("b000100???????????????????????")
//     def ALSL_W      = BitPat("b000000000000010??????????????")
//     def ALSL_WU     = BitPat("b000000000000011??????????????")
//     def ALSL_D      = BitPat("b00000000001011???????????????")
//     def LU12I_W     = BitPat("b0001010??????????????????????")
//     def LU32I_D     = BitPat("b0001011??????????????????????")
//     def LU52I_D     = BitPat("b0000001100???????????????????")
// }

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

object FuncOp {
    def add = "b000".U
    def sub = "b001".U
    def sl  = "b010".U
    def sr  = "b011".U
    def and = "b100".U
    def or  = "b101".U
    def xor = "b110".U
    def apply() = UInt(3.W)
}

object ")
file://<WORKSPACE>/src/main/scala/config/Configs.scala
file://<WORKSPACE>/src/main/scala/config/Configs.scala:85: error: expected identifier; obtained eof
object 
       ^
#### Short summary: 

expected identifier; obtained eof