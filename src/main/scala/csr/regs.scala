package csr

import chisel3._
import chisel3.util._

import isa._
import config._

object rwType {
  val R  = 0.U(2.W)
  val W1 = 1.U(2.W)
  val R0 = 2.U(2.W)
  val RW = 3.U(2.W)
}

trait base {
  val id: UInt
  val info: Data
  val rw: Data
  def write(value: UInt) = {
    info := value.asTypeOf(info)
  }
}

class CRMD_info extends Bundle {
  val zero = UInt(23.W)
  // val we   = Bool()    // 指令和数据监视点使能
  val datm = UInt(2.W) // 直接地址翻译模式时，load和store的存储访问类型
  val datf = UInt(2.W) // 直接地址翻译模式时，取值操作的存储访问类型
  val pg   = Bool() // 映射地址翻译使能
  val da   = Bool() // 直接地址翻译使能
  val ie   = Bool() // 全局中断使能
  val plv  = UInt(2.W) // 特权等级
}

class CRMD_rwType extends Bundle {
  val zero = rwType.R0
  // val we   = Bool()    // 指令和数据监视点使能
  val datm = rwType.RW // 直接地址翻译模式时，load和store的存储访问类型
  val datf = rwType.RW // 直接地址翻译模式时，取值操作的存储访问类型
  val pg   = rwType.RW // 映射地址翻译使能
  val da   = rwType.RW // 直接地址翻译使能
  val ie   = rwType.RW // 全局中断使能
  val plv  = rwType.RW // 特权等级
}

class CRMD extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new CRMD_info))
    init.da := true.B
    init
  })
  override val id = CSR.CRMD
  override val rw = new CRMD_rwType
}

//-------------------------------------------------------------------------

class PRMD_info extends Bundle {
  val zero = UInt(29.W)
  // val pwe  = Bool()    // 指令和数据监视点使能
  val pie  = Bool()    // 全局中断使能
  val pplv = UInt(2.W) // 特权等级
}

class PRMD_rwType extends Bundle {
  val zero = rwType.R0
  // val pwe  = Bool()    // 指令和数据监视点使能
  val pie  = rwType.RW // 全局中断使能
  val pplv = rwType.RW // 特权等级
}

class PRMD extends base {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new PRMD_info))})
  override val id = CSR.PRMD
  override val rw = new PRMD_rwType
}

//-------------------------------------------------------------------------

class EUEN_info extends Bundle {
  val zero = UInt(31.W)
  // val bte  = Bool()
  // val asxe = Bool()
  // val sxe  = Bool()
  val fpe  = Bool()
}

class EUEN_rwType extends Bundle {
  val zero = rwType.R0
  // val bte  = Bool()
  // val asxe = Bool()
  // val sxe  = Bool()
  val fpe  = rwType.RW
}

class EUEN extends base {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new EUEN_info))})
  override val id = CSR.EUEN
  override val rw = new EUEN_rwType
}

//-------------------------------------------------------------------------

class ECFG_info extends Bundle {
  val zero2  = UInt(19.W)
  // val vs    = UInt(3.W)
  val lie_12_11 = UInt(2.W)
  val zero1  = Bool()
  val lie_9_0 = UInt(10.W)
}

class ECFG_rwType extends Bundle {
  val zero2  = rwType.R0
  // val vs    = rwType.RW
  val lie_12_11 = rwType.RW
  val zero1  = rwType.R0
  val lie_9_0 = rwType.RW
}

class ECFG extends base {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new ECFG_info))})
  override val id = CSR.ECFG
  override val rw = new ECFG_rwType
}

//-------------------------------------------------------------------------

class ESTAT_info extends Bundle {
  val zero3    = Bool()
  val esubcode = UInt(9.W) 
  val ecode    = UInt(6.W) 
  val zero2    = UInt(3.W)
  val is_12    = Bool()
  val is_11    = Bool()
  val zero1    = Bool()
  val is_9_2   = UInt(8.W)
  val is_1_0   = UInt(2.W)
}

class ESTAT_rwType extends Bundle {
  val zero3    = rwType.R0
  val esubcode = rwType.R
  val ecode    = rwType.R
  val zero2    = rwType.R0
  val is_12    = rwType.R
  val is_11    = rwType.R
  val zero1    = rwType.R0
  val is_9_2   = rwType.R
  val is_1_0   = rwType.RW
}

class ESTAT extends base {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new ESTAT_info))})
  override val id = CSR.ESTAT
  override val rw = new ESTAT_rwType
}

//-------------------------------------------------------------------------

class ERA_info extends Bundle with Parameters {
  val pc = UInt(ADDR_WIDTH.W)
}

class ERA_rwType extends Bundle {
  val pc = rwType.RW
}

class ERA extends base {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new ERA_info))})
  override val id = CSR.ERA
  override val rw = new ERA_rwType
}

//-------------------------------------------------------------------------

class BADV_info extends Bundle with Parameters {
  val vaddr = UInt(ADDR_WIDTH.W)
}

class BADV_rwType extends Bundle {
  val vaddr = rwType.RW
}

class BADV extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new BADV_info))})
  override val id = CSR.BADV
  override val rw = new BADV_rwType
}

//-------------------------------------------------------------------------

class EENTRY_info extends Bundle with Parameters {
  val va  = UInt(26.W)
  val zero = UInt(6.W)
}

class EENTRY_rwType extends Bundle {
  val vpn  = rwType.RW
  // val zero = rwType.R //写被忽略，so为什么不写成下面这个形式
  val zero = rwType.R0
}

class EENTRY extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new EENTRY_info))})
  override val id = CSR.EENTRY
  override val rw = new EENTRY_rwType
}

//-------------------------------------------------------------------------

class SAVE0_info extends Bundle with Parameters {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE0_rwType extends Bundle {
  val data = rwType.RW
}

class SAVE0 extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new SAVE0_info))})
  override val id = CSR.SAVE0
  override val rw = new SAVE0_rwType
}

class SAVE1_info extends Bundle with Parameters {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE1_rwType extends Bundle {
  val data = rwType.RW
}

class SAVE1 extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new SAVE1_info))})
  override val id = CSR.SAVE1
  override val rw = new SAVE1_rwType
}

class SAVE2_info extends Bundle with Parameters {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE2_rwType extends Bundle {
  val data = rwType.RW
}

class SAVE2 extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new SAVE2_info))})
  override val id = CSR.SAVE2
  override val rw = new SAVE2_rwType
}

class SAVE3_info extends Bundle with Parameters {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE3_rwType extends Bundle {
  val data = rwType.RW
}

class SAVE3 extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new SAVE3_info))})
  override val id = CSR.SAVE3
  override val rw = new SAVE3_rwType
}

//-------------------------------------------------------------------------

class TID_info extends Bundle with Parameters {
  val tid = UInt(32.W)
}

class TID_rwType extends Bundle {
  val tid = rwType.RW
}

class TID extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new TID_info))})
  override val id = CSR.TID
  override val rw = new TID_rwType
}

//-------------------------------------------------------------------------

class TCFG_info extends Bundle with Parameters {
  val zero     = UInt((32 - COUNT_N).W)
  val initval  = UInt((COUNT_N - 2).W)
  val preiodic = Bool()
  val en       = Bool()
}

class TCFG_rwType extends Bundle {
  // val zero     = rwType.R//写被忽略，so为什么不写成下面这个形式
  val zero     = rwType.R0
  val initval  = rwType.RW
  val preiodic = rwType.RW
  val en       = rwType.RW
}

class TCFG extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new TCFG_info))})
  override val id = CSR.TCFG
  override val rw: Data = new TCFG_rwType
}

//-------------------------------------------------------------------------

class TVAL_info extends Bundle with Parameters {
  val zero    = UInt((32 - COUNT_N).W)
  val timeval = UInt(COUNT_N.W)
}

class TVAL_rwType extends Bundle {
  // val zero     = rwType.R//写被忽略，so为什么不写成下面这个形式
  val zero    = rwType.R0
  val timeval = rwType.R
}

class TVAL extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new TVAL_info))})
  override val id = CSR.TVAL
  override val rw = new TVAL_rwType
}

//-------------------------------------------------------------------------

class TICLR_info extends Bundle with Parameters {
  val zero = UInt(31.W)
  val clr  = Bool()
}

class TICLR_rwType extends Bundle {
  val zero = rwType.R0
  val clr  = rwType.W1
}

class TICLR extends base with Parameters {
  override val info = RegInit({WireDefault(0.U.asTypeOf(new TICLR_info))})
  override val id = CSR.TICLR
  override val rw = new TICLR_rwType
}