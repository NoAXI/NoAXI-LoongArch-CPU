package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class CRMD extends Bundle {
  val zero = UInt(22.W)
  val we   = Bool()    // 指令和数据监视点使能
  val datm = UInt(2.W) // 直接地址翻译模式时，load和store的存储访问类型
  val datf = UInt(2.W) // 直接地址翻译模式时，取值操作的存储访问类型
  val pg   = Bool()    // 映射地址翻译使能
  val da   = Bool()    // 直接地址翻译使能
  val ie   = Bool()    // 全局中断使能
  val plv  = UInt(2.W) // 特权等级
}

class PRMD extends Bundle {
  val zero = UInt(28.W)
  val pwe  = Bool()
  val pie  = Bool()
  val pplv = UInt(2.W)
}

class ECFG extends Bundle {
  val zero1 = UInt(13.W)
  val vs    = UInt(3.W)
  val zero2 = UInt(3.W)
  val lie   = UInt(13.W)
}

class ESTAT extends Bundle {
  val zero1    = UInt(1.W)
  val esubcode = UInt(9.W)  // 例外类型二级编码
  val ecode    = UInt(6.W)  // 例外类型一级编码
  val zero2    = UInt(3.W)
  val is       = UInt(13.W) // 软件中断状态位
}

class ERA extends Bundle with Parameters {
  val pc = UInt(ADDR_WIDTH.W)
}

class EENTRY extends Bundle with Parameters {
  val vpn  = UInt((ADDR_WIDTH - 12).W)
  val zero = UInt(12.W)
}

object f {
  def enter(csr: CSR, info: info): Unit = {
    csr.PRMD.pplv := csr.CRMD.plv
    csr.PRMD.pie  := csr.CRMD.ie
    csr.CRMD.plv  := 0.U
    csr.CRMD.ie   := 0.U
    // 应该是要填例外类型的ecode
    csr.ESTAT.ecode := MateDefault(
      info.op_type,
      0.U,
      List(
        ExcOpType.sys -> ECodes.SYS,
      ),
    )
    csr.ERA := info.pc

    csr.io.exc_bus.en := true.B
    csr.io.exc_bus.pc := entryPC(csr)
  }

  def back(csr: CSR): Unit = {
    csr.CRMD.plv := csr.PRMD.pplv
    csr.CRMD.ie  := csr.PRMD.pie

    // csr.io.exc_bus.en     := true.B
    // csr.io.exc_bus.exc_pc := csr.ERA
  }

  def entryPC(csr: CSR): UInt = {
    // csr.EENTRY | Mux(csr.ECFG.vs === 0.U, xxx, info.ecode << (x.reg(CSR.ECFG).vs + 2.U))
    csr.EENTRY.asUInt | ECodes.SYS << (csr.ECFG.vs + 2.U)
  }
}

class CSR_IO extends Bundle with Parameters {
  val re      = Input(Bool())
  val raddr   = Input(UInt(14.W))
  val rf_bus  = Input(new rf_bus)
  val info    = Input(new info)
  val rdata   = Output(UInt(32.W))
  val exc_bus = Output(new exc_bus)

  val start = Input(Bool())
  val end   = Input(Bool())
}

class CSR extends Module with Parameters {
  val io = IO(new CSR_IO)

  val CRMD = RegInit(0.U.asTypeOf(new CRMD))
  CRMD.da := RegInit(true.B) // 真的，你让我de了很久，众里寻他千百度，蓦然回首，却藏在指令手册深处
  val PRMD      = RegInit(0.U.asTypeOf(new PRMD))
  val ESTAT     = RegInit(0.U.asTypeOf(new ESTAT))
  val ERA       = RegInit(0.U.asTypeOf(new ERA))
  val EENTRY    = RegInit(0.U.asTypeOf(new EENTRY))
  val ECFG      = RegInit(0.U.asTypeOf(new ECFG))

  val EUEN      = RegInit(0.U(32.W))
  val BADV      = RegInit(0.U(32.W))
  val TLBIDX    = RegInit(0.U(32.W))
  val TLBEHI    = RegInit(0.U(32.W))
  val TLBELO0   = RegInit(0.U(32.W))
  val TLBELO1   = RegInit(0.U(32.W))
  val ASID      = RegInit(0.U(32.W))
  val PGDL      = RegInit(0.U(32.W))
  val PGDH      = RegInit(0.U(32.W))
  val PGD       = RegInit(0.U(32.W))
  val CPUID     = RegInit(0.U(32.W))
  val SAVE0     = RegInit(0.U(32.W))
  val SAVE1     = RegInit(0.U(32.W))
  val SAVE2     = RegInit(0.U(32.W))
  val SAVE3     = RegInit(0.U(32.W))
  val TID       = RegInit(0.U(32.W))
  val TCFG      = RegInit(0.U(32.W))
  val TVAL      = RegInit(0.U(32.W))
  val TICLR     = RegInit(0.U(32.W))
  val LLBCTL    = RegInit(0.U(32.W))
  val TLBRENTRY = RegInit(0.U(32.W))
  val CTAG      = RegInit(0.U(32.W))
  val DMW0      = RegInit(0.U(32.W))
  val DMW1      = RegInit(0.U(32.W))

  io.rdata := Mux(
    io.re,
    MateDefault(
      io.raddr,
      0.U,
      List(
        CSR.CRMD   -> CRMD.asUInt,
        CSR.PRMD   -> PRMD.asUInt,
        CSR.ESTAT  -> ESTAT.asUInt,
        CSR.ERA    -> ERA.asUInt,
        CSR.EENTRY -> EENTRY.asUInt,
        CSR.ECFG   -> ECFG.asUInt,
      ),
    ),
    0.U,
  )

  when(io.rf_bus.we) {
    switch(io.rf_bus.waddr) {
      is(CSR.CRMD) {
        val wdata = writeMask(io.rf_bus.wmask, CRMD.asUInt, io.rf_bus.wdata)
        CRMD := wdata.asTypeOf(new CRMD)
      }
      is(CSR.PRMD) {
        val wdata = writeMask(io.rf_bus.wmask, PRMD.asUInt, io.rf_bus.wdata)
        PRMD := wdata.asTypeOf(new PRMD)
      }
      is(CSR.ESTAT) {
        val wdata = writeMask(io.rf_bus.wmask, ESTAT.asUInt, io.rf_bus.wdata)
        ESTAT := wdata.asTypeOf(new ESTAT)
      }
      is(CSR.ERA) {
        val wdata = writeMask(io.rf_bus.wmask, ERA.asUInt, io.rf_bus.wdata)
        ERA := wdata.asTypeOf(new ERA)
      }
      is(CSR.EENTRY) {
        val wdata = writeMask(io.rf_bus.wmask, EENTRY.asUInt, io.rf_bus.wdata)
        EENTRY := wdata.asTypeOf(new EENTRY)
      }
      is(CSR.ECFG) {
        val wdata = writeMask(io.rf_bus.wmask, ECFG.asUInt, io.rf_bus.wdata)
        ECFG := wdata.asTypeOf(new ECFG)
      }
      is(CSR.EUEN) {
        val wdata = writeMask(io.rf_bus.wmask, EUEN, io.rf_bus.wdata)
        EUEN := wdata
      }
      is(CSR.BADV) {
        val wdata = writeMask(io.rf_bus.wmask, BADV, io.rf_bus.wdata)
        BADV := wdata
      }
      is(CSR.TLBIDX) {
        val wdata = writeMask(io.rf_bus.wmask, TLBIDX, io.rf_bus.wdata)
        TLBIDX := wdata
      }
      is(CSR.TLBEHI) {
        val wdata = writeMask(io.rf_bus.wmask, TLBEHI, io.rf_bus.wdata)
        TLBEHI := wdata
      }
      is(CSR.TLBELO0) {
        val wdata = writeMask(io.rf_bus.wmask, TLBELO0, io.rf_bus.wdata)
        TLBELO0 := wdata
      }
      is(CSR.TLBELO1) {
        val wdata = writeMask(io.rf_bus.wmask, TLBELO1, io.rf_bus.wdata)
        TLBELO1 := wdata
      }
      is(CSR.ASID) {
        val wdata = writeMask(io.rf_bus.wmask, ASID, io.rf_bus.wdata)
        ASID := wdata
      }
      is(CSR.PGDL) {
        val wdata = writeMask(io.rf_bus.wmask, PGDL, io.rf_bus.wdata)
        PGDL := wdata
      }
      is(CSR.PGDH) {
        val wdata = writeMask(io.rf_bus.wmask, PGDH, io.rf_bus.wdata)
        PGDH := wdata
      }
      is(CSR.PGD) {
        val wdata = writeMask(io.rf_bus.wmask, PGD, io.rf_bus.wdata)
        PGD := wdata
      }
      is(CSR.CPUID) {
        val wdata = writeMask(io.rf_bus.wmask, CPUID, io.rf_bus.wdata)
        CPUID := wdata
      }
      is(CSR.SAVE0) {
        val wdata = writeMask(io.rf_bus.wmask, SAVE0, io.rf_bus.wdata)
        SAVE0 := wdata
      }
      is(CSR.SAVE1) {
        val wdata = writeMask(io.rf_bus.wmask, SAVE1, io.rf_bus.wdata)
        SAVE1 := wdata
      }
      is(CSR.SAVE2) {
        val wdata = writeMask(io.rf_bus.wmask, SAVE2, io.rf_bus.wdata)
        SAVE2 := wdata
      }
      is(CSR.SAVE3) {
        val wdata = writeMask(io.rf_bus.wmask, SAVE3, io.rf_bus.wdata)
        SAVE3 := wdata
      }
      is(CSR.TID) {
        val wdata = writeMask(io.rf_bus.wmask, TID, io.rf_bus.wdata)
        TID := wdata
      }
      is(CSR.TCFG) {
        val wdata = writeMask(io.rf_bus.wmask, TCFG, io.rf_bus.wdata)
        TCFG := wdata
      }
      is(CSR.TVAL) {
        val wdata = writeMask(io.rf_bus.wmask, TVAL, io.rf_bus.wdata)
        TVAL := wdata
      }
      is(CSR.TICLR) {
        val wdata = writeMask(io.rf_bus.wmask, TICLR, io.rf_bus.wdata)
        TICLR := wdata
      }
      is(CSR.LLBCTL) {
        val wdata = writeMask(io.rf_bus.wmask, LLBCTL, io.rf_bus.wdata)
        LLBCTL := wdata
      }
      is(CSR.TLBRENTRY) {
        val wdata = writeMask(io.rf_bus.wmask, TLBRENTRY, io.rf_bus.wdata)
        TLBRENTRY := wdata
      }
      is(CSR.CTAG) {
        val wdata = writeMask(io.rf_bus.wmask, CTAG, io.rf_bus.wdata)
        CTAG := wdata
      }
      is(CSR.DMW0) {
        val wdata = writeMask(io.rf_bus.wmask, DMW0, io.rf_bus.wdata)
        DMW0 := wdata
      }
      is(CSR.DMW1) {
        val wdata = writeMask(io.rf_bus.wmask, DMW1, io.rf_bus.wdata)
        DMW1 := wdata
      }
    }
  }

  CRMD.zero   := 0.U
  PRMD.zero   := 0.U
  ESTAT.zero1 := 0.U
  ESTAT.zero2 := 0.U
  EENTRY.zero := 0.U
  ECFG.zero1  := 0.U
  ECFG.zero2  := 0.U

  io.exc_bus := WireDefault(0.U.asTypeOf(new exc_bus))
  when (io.start) {
    PRMD.pplv := CRMD.plv
    PRMD.pie  := CRMD.ie
    CRMD.plv  := 0.U
    CRMD.ie   := 0.U
    ESTAT.ecode := MateDefault(
      io.info.op_type,
      0.U,
      List(
        ExcOpType.sys -> ECodes.SYS,
      ),
    )
    ERA.pc := io.info.pc

    io.exc_bus.en := true.B
    io.exc_bus.pc := EENTRY.asUInt
  }

  when (io.end) {
    CRMD.plv := PRMD.pplv
    CRMD.ie  := PRMD.pie

    io.exc_bus.en := true.B
    io.exc_bus.pc := ERA.pc
  }
}
