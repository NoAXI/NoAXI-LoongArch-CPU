package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import const.tlbConst._
import const.Parameters._
import func.Functions._

object rwType {
  val R  = 0.U(2.W)
  val W1 = 1.U(2.W)
  val R0 = 2.U(2.W)
  val RW = 3.U(2.W)
}

trait base {
  val id: UInt
  val info: Data
  val rw: UInt
  def write(value: UInt) = {
    info := writeMask(rw, info.asUInt, value).asTypeOf(info)
  }
}

class CRMD_info extends Bundle {
  val zero = UInt(23.W)
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
  }).suggestName("CRMD")
  override val id = CSRCodes.CRMD
  override val rw = "b0000_0000_0000_0000_0000_0001_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class PRMD_info extends Bundle {
  val zero = UInt(29.W)
  // val pwe  = Bool()    // 指令和数据监视点使能
  val pie  = Bool()    // 全局中断使能
  val pplv = UInt(2.W) // 特权等级
}

class PRMD extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new PRMD_info)) }).suggestName("PRMD")
  override val id   = CSRCodes.PRMD
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0111".U(32.W)
}

//-------------------------------------------------------------------------

class EUEN_info extends Bundle {
  val zero = UInt(31.W)
  val fpe  = Bool()
}

class EUEN extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new EUEN_info)) }).suggestName("EUEN")
  override val id   = CSRCodes.EUEN
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0001".U(32.W)
}

//-------------------------------------------------------------------------

class ECFG_info extends Bundle {
  val zero2     = UInt(19.W)
  val lie_12_11 = UInt(2.W)
  val zero1     = Bool()
  val lie_9_0   = UInt(10.W)
}

class ECFG extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new ECFG_info)) }).suggestName("ECFG")
  override val id   = CSRCodes.ECFG
  override val rw   = "b0000_0000_0000_0000_0001_1011_1111_1111".U(32.W)
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

class ESTAT extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new ESTAT_info)) }).suggestName("ESTAT")
  override val id   = CSRCodes.ESTAT
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0011".U(32.W)
}

//-------------------------------------------------------------------------

class ERA_info extends Bundle {
  val pc = UInt(ADDR_WIDTH.W)
}

class ERA extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new ERA_info)) }).suggestName("ERA")
  override val id   = CSRCodes.ERA
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class BADV_info extends Bundle {
  val vaddr = UInt(ADDR_WIDTH.W)
}

class BADV extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new BADV_info)) }).suggestName("BADV")
  override val id   = CSRCodes.BADV
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class EENTRY_info extends Bundle {
  val va   = UInt(26.W)
  val zero = UInt(6.W)
}

class EENTRY extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new EENTRY_info)) }).suggestName("EENTRY")
  override val id   = CSRCodes.EENTRY
  override val rw   = "b1111_1111_1111_1111_1111_1111_1100_0000".U(32.W)
}

//-------------------------------------------------------------------------

class TLBIDX_info extends Bundle {
  val ne    = Bool()
  val zero1 = Bool()
  val ps    = UInt(6.W)
  val zero2 = UInt((24 - TLB_INDEX_LEN).W)
  val index = UInt(TLB_INDEX_LEN.W)
}

class TLBIDX extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TLBIDX_info)) }).suggestName("TLBIDX")
  override val id   = CSRCodes.TLBIDX
  override val rw   = if (Config.debug_on_chiplab) "b1011_1111_0000_0000_0000_0000_0001_1111".U(32.W) else "b1011_1111_0000_0000_0000_0000_0000_1111".U(32.W)
}

//-------------------------------------------------------------------------

class TLBEHI_info extends Bundle {
  val vppn  = UInt(19.W)
  val zero1 = UInt(13.W)
}

class TLBEHI extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TLBEHI_info)) }).suggestName("TLBEHI")
  override val id   = CSRCodes.TLBEHI
  override val rw   = "b1111_1111_1111_1111_1110_0000_0000_0000".U(32.W)
}

//-------------------------------------------------------------------------

class TLBELO_info extends Bundle {
  val zero2 = UInt(4.W)
  val ppn   = UInt(20.W)
  val zero1 = Bool()
  val g     = Bool()
  val mat   = UInt(2.W)
  val plv   = UInt(2.W)
  val d     = Bool()
  val v     = Bool()
}

class TLBELO0 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TLBELO_info)) }).suggestName("TLBELO0")
  override val id   = CSRCodes.TLBELO0
  override val rw   = "b0000_1111_1111_1111_1111_1111_0111_1111".U(32.W)
}

class TLBELO1 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TLBELO_info)) }).suggestName("TLBELO1")
  override val id   = CSRCodes.TLBELO1
  override val rw   = "b0000_1111_1111_1111_1111_1111_0111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class ASID_info extends Bundle {
  val zero2    = UInt(8.W)
  val asidbits = UInt(8.W)
  val zero1    = UInt(6.W)
  val asid     = UInt(10.W)
}

// very strange bug, why the asidbits can be changed
// func_test 1c00f614
class ASID extends base {
  override val info = RegInit({ 0x000a0000.U.asTypeOf(new ASID_info) }).suggestName("ASID")
  override val id   = CSRCodes.ASID
  override val rw   = "b0000_0000_0000_0000_0000_0011_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class PGDL_info extends Bundle {
  val base = UInt(20.W)
  val zero = UInt(12.W)
}

class PGDL extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new PGDL_info)) }).suggestName("PGDL")
  override val id   = CSRCodes.PGDL
  override val rw   = "b1111_1111_1111_1111_1111_0000_0000_0000".U(32.W)
}

//-------------------------------------------------------------------------

class PGDH_info extends Bundle {
  val base = UInt(20.W)
  val zero = UInt(12.W)
}

class PGDH extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new PGDH_info)) }).suggestName("PGDH")
  override val id   = CSRCodes.PGDH
  override val rw   = "b1111_1111_1111_1111_1111_0000_0000_0000".U(32.W)
}

//-------------------------------------------------------------------------

class PGD_info extends Bundle {
  val base = UInt(20.W)
  val zero = UInt(12.W)
}

class PGD extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new PGD_info)) }).suggestName("PGD")
  override val id   = CSRCodes.PGD
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0000".U(32.W)
}

//-------------------------------------------------------------------------

class SAVE0_info extends Bundle {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE0 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new SAVE0_info)) }).suggestName("SAVE0")
  override val id   = CSRCodes.SAVE0
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

class SAVE1_info extends Bundle {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE1_rwType extends Bundle {
  val data = 1.U(32.W)
}

class SAVE1 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new SAVE1_info)) }).suggestName("SAVE1")
  override val id   = CSRCodes.SAVE1
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

class SAVE2_info extends Bundle {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE2 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new SAVE2_info)) }).suggestName("SAVE2")
  override val id   = CSRCodes.SAVE2
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

class SAVE3_info extends Bundle {
  val data = UInt(DATA_WIDTH.W)
}

class SAVE3 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new SAVE3_info)) }).suggestName("SAVE3")
  override val id   = CSRCodes.SAVE3
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class CPUID_info extends Bundle {
  val zero   = UInt(23.W)
  val coreid = UInt(9.W)
}

class CPUID extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new CPUID_info)) }).suggestName("CPUID")
  override val id   = CSRCodes.CPUID
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0000".U(32.W)
}

//-------------------------------------------------------------------------

class TID_info extends Bundle {
  val tid = UInt(32.W)
}

class TID extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TID_info)) }).suggestName("TID")
  override val id   = CSRCodes.TID
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class TCFG_info extends Bundle {
  val zero     = UInt((32 - COUNT_N).W)
  val initval  = UInt((COUNT_N - 2).W)
  val preiodic = Bool()
  val en       = Bool()
}

class TCFG extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TCFG_info)) }).suggestName("TCFG")
  override val id   = CSRCodes.TCFG
  override val rw   = "b0000_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class TVAL_info extends Bundle {
  val zero    = UInt((32 - COUNT_N).W)
  val timeval = UInt(COUNT_N.W)
}

class TVAL extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TVAL_info)) }).suggestName("TVAL")
  override val id   = CSRCodes.TVAL
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0000".U(32.W)
}

//-------------------------------------------------------------------------

class TICLR_info extends Bundle {
  val zero = UInt(31.W)
  val clr  = Bool()
}

class TICLR extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TICLR_info)) }).suggestName("TICLR")
  override val id   = CSRCodes.TICLR
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0001".U(32.W)
}

//-------------------------------------------------------------------------

class LLBCTL_info extends Bundle {
  val zero  = UInt(29.W)
  val klo   = Bool()
  val wcllb = Bool() // 读出没有任何意义
  val rollb = Bool() // llbit的值
}

class LLBCTL extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new LLBCTL_info)) }).suggestName("LLBCTL")
  override val id   = CSRCodes.LLBCTL
  override val rw   = "b0000_0000_0000_0000_0000_0000_0000_0100".U(32.W)
}

//-------------------------------------------------------------------------

class TLBRENTRY_info extends Bundle {
  val pa   = UInt(26.W)
  val zero = UInt(6.W)
}

class TLBRENTRY extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new TLBRENTRY_info)) }).suggestName("TLBRENTRY")
  override val id   = CSRCodes.TLBRENTRY
  override val rw   = "b1111_1111_1111_1111_1111_1111_1100_0000".U(32.W)
}

//-------------------------------------------------------------------------

class CTAG_info extends Bundle {
  val info = UInt(32.W) // TODO: unknown
}

class CTAG extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new CTAG_info)) }).suggestName("CTAG")
  override val id   = CSRCodes.CTAG
  override val rw   = "b1111_1111_1111_1111_1111_1111_1111_1111".U(32.W)
}

//-------------------------------------------------------------------------

class DMW_info extends Bundle {
  val vseg  = UInt(3.W)
  val zero3 = Bool()
  val pseg  = UInt(3.W)
  val zero2 = UInt(19.W)
  val mat   = UInt(2.W)
  val plv3  = Bool()
  val zero1 = UInt(2.W)
  val plv0  = Bool()
}

class DMW0 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new DMW_info)) }).suggestName("DMW0")
  override val id   = CSRCodes.DMW0
  override val rw   = "b1110_1110_0000_0000_0000_0000_0011_1001".U(32.W)
}

//-------------------------------------------------------------------------

class DMW1 extends base {
  override val info = RegInit({ WireDefault(0.U.asTypeOf(new DMW_info)) }).suggestName("DMW1")
  override val id   = CSRCodes.DMW1
  override val rw   = "b1110_1110_0000_0000_0000_0000_0011_1001".U(32.W)
}

//-------------------------------------------------------------------------
