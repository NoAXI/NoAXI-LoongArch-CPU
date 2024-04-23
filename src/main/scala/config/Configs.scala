package config

import chisel3._
import chisel3.util._

import math._

class info extends Bundle {
    val pc              = UInt(32.W)
    val inst            = UInt(32.W)
    val alu_op          = UInt(6.W)
    val load_op         = UInt(1.W)
    val src1_is_pc      = UInt(1.W)
    val src2_is_imm     = UInt(1.W)
    val src2_is_4       = UInt(1.W)
    val gr_we           = UInt(1.W)
    val mem_we          = UInt(1.W)
    val dest            = UInt(5.W)
    val imm             = UInt(32.W)
    val rj_value        = UInt(32.W)
    val rkd_value       = UInt(32.W)
    val res_from_mem    = UInt(1.W)
    val alu_result      = UInt(32.W)
}

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

    //前一个阶段和这个阶段握手，并获得数据
    def ConnectGetBus (x: DecoupledIO[info], y: DecoupledIO[info]): info = {
        val info = RegInit(0.U.asTypeOf(new info))
        val valid = RegInit(false.B)
        val ready_go = true.B
        x.ready := !valid || ready_go && y.ready
        y.valid := valid && ready_go
        when (x.ready) {
            valid := x.valid
        }
        when (x.valid && x.ready) {
            info := x.bits
        }
        info
    }

    // object SignExt {
    // def apply(a: UInt, len: Int) = {
    //     val aLen = a.getWidth
    //     val signBit = a(aLen-1)
    //     if (aLen >= len) a(len-1,0) else Cat(Fill(len - aLen, signBit), a)
    // }
    // }

    // object ZeroExt {
    // def apply(a: UInt, len: Int) = {
    //     val aLen = a.getWidth
    //     if (aLen >= len) a(len-1,0) else Cat(0.U((len - aLen).W), a)
    // }
    // }

    // def Mate[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
    //     Mux1H(mapping.map(p => (p._1 === key, p._2)))

    def lookup[T <: Data](key: UInt, default: T, map: Array[(BitPat, T)]): T  = {
        val result = WireDefault(default)
        for ((pattern, value) <- map) {
            when (key === pattern) {
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
    val DATA_WIDTH_D = 64  // 双字
    val DATA_WIDTH_W = DATA_WIDTH_D / 2  // 字
    val DATA_WIDTH_H = DATA_WIDTH_W / 2  // 半字
    val DATA_WIDTH_B = DATA_WIDTH_H / 2  // 字节
    // val DATA_WIDTH_b = 1 // 位

    val INST_WIDTH = 32  // 指令长度
    val INST_WIDTH_B = 32 / 8  // 指令字节长度

    val GR_SIZE = 32  // 通用寄存器数量
    val GR_LEN = 64  // 通用寄存器长度(位宽)
    
    val START_ADDR = 0x1bfffffc  // 程序起始地址
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
