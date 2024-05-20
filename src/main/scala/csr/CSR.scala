package csr

import chisel3._
import chisel3.util._

import bundles._
import const._
import const.Parameters._
import Funcs.Functions._

class CSR_IO extends Bundle {
  // act with ds
  val csr_reg_read = Flipped(new csrRegRead)

  // from wb
  val exc_happen   = Input(new excHappen)
  val flush_by_csr = Output(Bool())
  val csr_write    = Input(new CSRWrite)

  // to fs
  val br_exc = Output(new br)
}

class CSR extends Module {
  val io = IO(new CSR_IO)

  val saved_info = RegInit(0.U.asTypeOf(new info))
  when (!io.exc_happen.info.bubble) {
    saved_info := io.exc_happen.info
  }
  val info = Mux(io.exc_happen.info.bubble, saved_info, io.exc_happen.info)

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
  val SAVE0 = new SAVE0
  val SAVE1 = new SAVE1
  val SAVE2 = new SAVE2
  val SAVE3 = new SAVE3
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
    SAVE0,
    SAVE1,
    SAVE2,
    SAVE3,
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
  io.csr_reg_read.rdata := 0.U
  when(io.csr_reg_read.re) {
    for (x <- csrlist) {
      when(io.csr_reg_read.raddr === x.id) {
        io.csr_reg_read.rdata := x.info.asUInt
        when(x.id === CSRCodes.TICLR) {
          io.csr_reg_read.rdata := 0.U
        }
      }
    }
  }

  when(io.csr_write.we) {
    for (x <- csrlist) {
      when(io.csr_write.waddr === x.id) {
        val wdata = writeMask(io.csr_write.wmask, x.info.asUInt, io.csr_write.wdata)
        x.write(wdata)
        // 清除中断位 当有写1的行为
        when(x.id === CSRCodes.TICLR && wdata(0) === 1.U) {
          ESTAT.info.is_11 := false.B
        }
        when(x.id === CSRCodes.TCFG) {
          TVAL.info.timeval := wdata(COUNT_N - 1, 2) ## 1.U(2.W)
        }
      }
    }
  }

  when(TCFG.info.en) {
    when(TVAL.info.timeval === 0.U) {
      TVAL.info.timeval := Mux(TCFG.info.preiodic, TCFG.info.initval ## 1.U(2.W), 0.U)
    }.otherwise {
      TVAL.info.timeval := TVAL.info.timeval - 1.U
    }
  }

  val TVAL_edge = ShiftRegister(TVAL.info.timeval, 1)
  when(TCFG.info.en && TVAL.info.timeval === 0.U && TVAL_edge === 1.U) {
    ESTAT.info.is_11 := true.B
  }

  val any_exc = Cat(ESTAT.info.is_12, ESTAT.info.is_11, ESTAT.info.is_9_2, ESTAT.info.is_1_0) &
    Cat(ECFG.info.lie_12_11, ECFG.info.lie_9_0)
  val start = io.exc_happen.start || (any_exc.orR && CRMD.info.ie)

  // 例外跳转
  io.br_exc := WireDefault(0.U.asTypeOf(new br))
  io.flush_by_csr := false.B
  when(start) {
    io.flush_by_csr := true.B
    PRMD.info.pplv  := CRMD.info.plv
    PRMD.info.pie   := CRMD.info.ie
    CRMD.info.plv   := 0.U
    CRMD.info.ie    := 0.U
    // 中断>例外的优先级
    ESTAT.info.ecode := MuxCase(
      info.exc_type,
      List(
        ESTAT.info.is_1_0.orR -> ECodes.INT,
        ESTAT.info.is_9_2.orR -> ECodes.INT,
        ESTAT.info.is_11      -> ECodes.INT,
        ESTAT.info.is_12      -> ECodes.INT,
      ),
    )
    ESTAT.info.esubcode := Mux(info.exc_type === ECodes.ADEM, 1.U, 0.U)
    ERA.info.pc         := info.pc
    BADV.info.vaddr := MateDefault(
      info.exc_type,
      BADV.info.vaddr,
      List(
        ECodes.ADEF -> info.exc_vaddr,
        ECodes.ADEM -> info.exc_vaddr,
        ECodes.ALE  -> info.exc_vaddr,
      ),
    )

    io.br_exc.en  := true.B
    io.br_exc.tar := EENTRY.info.asUInt
  }

  when(io.exc_happen.end) {
    io.flush_by_csr := true.B
    CRMD.info.plv   := PRMD.info.pplv
    CRMD.info.ie    := PRMD.info.pie

    io.br_exc.en  := true.B
    io.br_exc.tar := ERA.info.pc
  }
}
