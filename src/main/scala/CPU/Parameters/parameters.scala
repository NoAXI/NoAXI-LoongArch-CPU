/*
变量命名规则：

包名:            驼峰命名法(首字母大写)
模块名：         驼峰命名法(首字母大写)
常规变量参量名:   全小写，单词之间用下划线分隔
信号名: 全小写，  单词之间用下划线分隔
常量名:          全大写，单词之间用下划线分隔

*/

package Parameters

import chisel3._
import chisel3.util._

import HandShake._

trait Parameters {
    val DATA_WIDTH = 64

    val INST_WIDTH = 32

    val START_ADDR = 0x1bfffffc
    val ADDR_WIDTH = 32
    val ADDR_WIDTH_REG = 5

    val REG_SIZE = 32
}

object Functions {
    def ConnetGetBus (x: HandShakeBf, y:HandShakeAf): Bus = {
        val bus = WireInit(0.U.asTypeOf(new Bus))
        val valid = RegInit(false.B)
        val ready_go = true.B
        x.ready_in := !valid || ready_go && y.ready_in
        y.valid_out := valid && ready_go
        when (x.ready_in) {
            valid := x.valid_out
        }
        when (x.valid_out && x.ready_in) {
            bus := x.bus_out
        }
        bus
    }

    def MateDefault[T <: Data](key: UInt, default: T, map: Iterable[(UInt, T)]): T =
        MuxLookup(key, default)(map.toSeq)

    def SignedExtend(a: UInt, len: Int) = {
        val aLen = a.getWidth
        val signBit = a(aLen-1)
        if (aLen >= len) a(len-1,0) else Cat(Fill(len - aLen, signBit), a)
    }

    def UnsignedExtend(a: UInt, len: Int) = {
        val aLen = a.getWidth
        if (aLen >= len) a(len-1,0) else Cat(0.U((len - aLen).W), a)
    }
}