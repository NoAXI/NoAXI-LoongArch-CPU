package csr

import chisel3._
import chisel3.util._

import isa._
import stages._
import config._
import config.Functions._

class CSR_IO extends Bundle with Parameters {
  // from ds
  val re     = Input(Bool())
  val raddr  = Input(UInt(14.W))
  val rf_bus = Input(new rf_bus)

  // from wb
  val info  = Input(new info)
  val start = Input(Bool())
  val end   = Input(Bool())

  // to ds
  val rdata = Output(UInt(DATA_WIDTH.W))

  // to fs
  val exc_bus = Output(new exc_bus)
}

class CSR extends Module with Parameters {
  val io = IO(new CSR_IO)

  val CRMD   = new CRMD
  val PRMD   = new PRMD
  val EUEN   = new EUEN
  val ECFG   = new ECFG
  val ESTAT  = new ESTAT
  val ERA    = new ERA
  val BADV   = new BADV
  val EENTRY = new EENTRY
  // val TLBIDX    =
  // val TLBEHI    =
  // val TLBELO0   =
  // val TLBELO1   =
  // val ASID      =
  // val PGDL      =
  // val PGDH      =
  // val PGD       =
  // val CPUID     =
  // val SAVE0     =
  // val SAVE1     =
  // val SAVE2     =
  // val SAVE3     =
  val TID   = new TID
  val TCFG  = new TCFG
  val TVAL  = new TVAL
  val TICLR = new TICLR
  // val LLBCTL    =
  // val TLBRENTRY =
  // val CTAG      =
  // val DMW0      =
  // val DMW1      =

  val csrlist = Seq(
    CRMD,
    PRMD,
    EUEN,
    ECFG,
    ESTAT,
    ERA,
    BADV,
    EENTRY,
    // TLBIDX,
    // TLBEHI,
    // TLBELO0,
    // TLBELO1,
    // ASID,
    // PGDL,
    // PGDH,
    // PGD,
    // CPUID,
    // SAVE0,
    // SAVE1,
    // SAVE2,
    // SAVE3,
    TID,
    TCFG,
    TVAL,
    TICLR,
    // LLBCTL,
    // TLBRENTRY,
    // CTAG,
    // DMW0,
    // DMW1,
  )

  // 读 or 写
  io.rdata := 0.U
  when(io.re) {
    for (x <- csrlist) {
      when(io.raddr === x.id) {
        io.rdata := x.info.asUInt
      }
    }
  }

  when(io.rf_bus.we) {
    for (x <- csrlist) {
      when(io.rf_bus.waddr === x.id) {
        x.write(writeMask(io.rf_bus.wmask, x.info.asUInt, io.rf_bus.wdata))
      }
    }
  }

  // 不可写区域
  CRMD.info.zero   := 0.U
  PRMD.info.zero   := 0.U
  EUEN.info.zero   := 0.U
  ECFG.info.zero1  := 0.U
  ECFG.info.zero2  := 0.U
  ESTAT.info.zero1 := 0.U
  ESTAT.info.zero2 := 0.U
  EENTRY.info.zero := 0.U
  TCFG.info.zero   := 0.U
  TVAL.info.zero   := 0.U
  TICLR.info.zero  := 0.U

  // 例外跳转
  io.exc_bus := WireDefault(0.U.asTypeOf(new exc_bus))
  when(io.start) {
    PRMD.info.pplv := CRMD.info.plv
    PRMD.info.pie  := CRMD.info.ie
    CRMD.info.plv  := 0.U
    CRMD.info.ie   := 0.U
    ESTAT.info.ecode := io.info.exc_type
    ERA.info.pc := io.info.pc

    io.exc_bus.en := true.B
    io.exc_bus.pc := EENTRY.info.asUInt
  }

  when(io.end) {
    CRMD.info.plv := PRMD.info.pplv
    CRMD.info.ie  := PRMD.info.pie

    io.exc_bus.en := true.B
    io.exc_bus.pc := ERA.info.pc
  }
}
