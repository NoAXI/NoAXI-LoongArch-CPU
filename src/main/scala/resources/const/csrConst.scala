package const

import chisel3._
import chisel3.util._

// CSR(控制状态寄存器) 手册P123
object CSRCodes {
  val CRMD      = 0x0.U(14.W)
  val PRMD      = 0x1.U(14.W)
  val EUEN      = 0x2.U(14.W)
  val ECFG      = 0x4.U(14.W)
  val ESTAT     = 0x5.U(14.W)
  val ERA       = 0x6.U(14.W)
  val BADV      = 0x7.U(14.W)
  val EENTRY    = 0xc.U(14.W)
  val TLBIDX    = 0x10.U(14.W)
  val TLBEHI    = 0x11.U(14.W)
  val TLBELO0   = 0x12.U(14.W)
  val TLBELO1   = 0x13.U(14.W)
  val ASID      = 0x18.U(14.W)
  val PGDL      = 0x19.U(14.W)
  val PGDH      = 0x1a.U(14.W)
  val PGD       = 0x1b.U(14.W)
  val CPUID     = 0x20.U(14.W)
  val SAVE0     = 0x30.U(14.W)
  val SAVE1     = 0x31.U(14.W)
  val SAVE2     = 0x32.U(14.W)
  val SAVE3     = 0x33.U(14.W)
  val TID       = 0x40.U(14.W)
  val TCFG      = 0x41.U(14.W)
  val TVAL      = 0x42.U(14.W)
  val TICLR     = 0x44.U(14.W)
  val LLBCTL    = 0x60.U(14.W)
  val TLBRENTRY = 0x88.U(14.W)
  val CTAG      = 0x98.U(14.W)
  val DMW0      = 0x180.U(14.W)
  val DMW1      = 0x181.U(14.W)
}

// 例外编码表
object ECodes {
  val INT  = 0x00.U(7.W) // interrupt
  val PIL  = 0x01.U(7.W) // page illegal load
  val PIS  = 0x02.U(7.W) // page illegal store
  val PIF  = 0x03.U(7.W) // page illegal fetch
  val PME  = 0x04.U(7.W) // page maintain exception
  val PPI  = 0x07.U(7.W) // page privilege illegal
  val ADEF = 0x08.U(7.W) // address exception fetch
  val ADEM = 0x48.U(7.W) // address exception memory
  val ALE  = 0x09.U(7.W) // address align exception
  val SYS  = 0x0b.U(7.W) // system call
  val BRK  = 0x0c.U(7.W) // breakpoint
  val INE  = 0x0d.U(7.W) // instruction not exist
  val IPE  = 0x0e.U(7.W) // instruction privilege exception
  val FPD  = 0x0f.U(7.W) // floating point disable
  val FPE  = 0x12.U(7.W) // floating point exception
  val TLBR = 0x3f.U(7.W) // TLB refill
  // add
  val NONE = 0x25.U(7.W) // no exception
  val ertn = 0x26.U(7.W) // exception return

  def apply()                       = UInt(7.W)
  def istlbException(x: UInt): Bool = VecInit(Seq(PIL, PIS, PIF, PME, PPI, TLBR)).contains(x)
}

// 线中断表
object WireBreak {
  val IPI  = 12.U(7.W)
  val TI   = 11.U(7.W)
  val PMI  = 10.U(7.W)
  val HWI0 = 9.U(7.W)
  val HWI1 = 8.U(7.W)
  val HWI2 = 7.U(7.W)
  val HWI3 = 6.U(7.W)
  val HWI4 = 5.U(7.W)
  val HWI5 = 4.U(7.W)
  val HWI6 = 3.U(7.W)
  val HWI7 = 2.U(7.W)
  val SWI0 = 1.U(7.W)
  val SWI1 = 0.U(7.W)
}
