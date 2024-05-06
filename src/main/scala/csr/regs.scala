package csr

import chisel3._
import chisel3.util._

import isa._
import config._

trait base {
  val id: UInt
  val info: Data
  def write(value: UInt) = {
    info := value.asTypeOf(info)
  }
}

class CRMD_info extends Bundle {
  val zero = UInt(22.W)
  val we   = Bool()    // 指令和数据监视点使能
  val datm = UInt(2.W) // 直接地址翻译模式时，load和store的存储访问类型
  val datf = UInt(2.W) // 直接地址翻译模式时，取值操作的存储访问类型
  val pg   = Bool()    // 映射地址翻译使能
  val da   = Bool()    // 直接地址翻译使能
  val ie   = Bool()    // 全局中断使能
  val plv  = UInt(2.W) // 特权等级
}

class CRMD extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new CRMD_info))
    init.da := true.B
    init
  })
  override val id = CSR.CRMD
}

class PRMD_info extends Bundle {
  val zero = UInt(28.W)
  val pwe  = Bool()    // 指令和数据监视点使能
  val pie  = Bool()    // 全局中断使能
  val pplv = UInt(2.W) // 特权等级
}

class PRMD extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new PRMD_info))
    init
  })
  override val id = CSR.PRMD
}

class EUEN_info extends Bundle {
  val zero = UInt(28.W)
  val bte  = Bool()
  val asxe = Bool()
  val sxe  = Bool()
  val fpe  = Bool()
}

class EUEN extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new EUEN_info))
    init
  })
  override val id = CSR.EUEN
}

class ECFG_info extends Bundle {
  val zero1 = UInt(13.W)
  val vs    = UInt(3.W)
  val zero2 = UInt(3.W)
  val lie   = UInt(13.W)
}

class ECFG extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new ECFG_info))
    init
  })
  override val id = CSR.ECFG
}

class ESTAT_info extends Bundle {
  val zero1    = UInt(1.W)
  val esubcode = UInt(9.W)  // 例外类型二级编码
  val ecode    = UInt(6.W)  // 例外类型一级编码
  val zero2    = UInt(3.W)
  val is       = UInt(13.W) // 软件中断状态位

}

class ESTAT extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new ESTAT_info))
    init
  })
  override val id = CSR.ESTAT
}

class ERA_info extends Bundle with Parameters {
  val pc = UInt(ADDR_WIDTH.W)
}

class ERA extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new ERA_info))
    init
  })
  override val id = CSR.ERA
}

class BADV_info extends Bundle with Parameters {
  val vaddr = UInt(ADDR_WIDTH.W)
}

class BADV extends base with Parameters {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new BADV_info))
    init
  })
  override val id = CSR.BADV
}

class EENTRY_info extends Bundle with Parameters {
  val vpn  = UInt((ADDR_WIDTH - 12).W)
  val zero = UInt(12.W)
}

class EENTRY extends base with Parameters {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new EENTRY_info))
    init
  })
  override val id = CSR.EENTRY
}

class TID_info extends Bundle with Parameters {
  val tid = UInt(32.W)
}

class TID extends base with Parameters {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new TID_info))
    init
  })
  override val id = CSR.TID
}

class TCFG_info extends Bundle with Parameters {
  val zero     = UInt((30 - COUNT_N).W)
  val initval  = UInt(COUNT_N.W)
  val preiodic = Bool()
  val en       = Bool()
}

class TCFG extends base with Parameters {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new TCFG_info))
    init
  })
  override val id = CSR.TCFG
}

class TVAL_info extends Bundle with Parameters {
  val zero    = UInt((32 - COUNT_N).W)
  val timeval = UInt(COUNT_N.W)
}

class TVAL extends base with Parameters {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new TVAL_info))
    init
  })
  override val id = CSR.TVAL
}

class TICLR_info extends Bundle with Parameters {
  val zero = UInt(31.W)
  val clr  = Bool()
}

class TICLR extends base with Parameters {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new TICLR_info))
    init
  })
  override val id = CSR.TICLR
}