package csr

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.TlbOpType

// TODO: IPE !

class CSRIO extends Bundle {
  // memory2
  val csrRead = new CsrReadIO

  // commit
  val excHappen = Input(new ExcHappenInfo)
  val csrWrite  = Input(new CSRWrite)
  val excJump   = Output(new BranchInfo)

  // to tlb
  val tlb = new CSRTLBIO

  // mark to dec
  val intExc = Output(Bool())
  val plv    = Output(UInt(2.W))

  // hard interrupt
  val ext_int = Input(UInt(8.W))

  // llbit
  val llbit = Output(Bool())
  val writeLLBCTL = Input(new Bundle {
    val en    = Bool()
    val wdata = Bool()
  })

  val csrRegs = if (Config.debug_on_chiplab) Some(Output(new DifftestCSRRegState)) else None
}

class CSR extends Module {
  val io = IO(new CSRIO)

  val info = io.excHappen.info

  val CRMD      = new CRMD
  val PRMD      = new PRMD
  val EUEN      = new EUEN
  val ECFG      = new ECFG
  val ESTAT     = new ESTAT
  val ERA       = new ERA
  val BADV      = new BADV
  val EENTRY    = new EENTRY
  val TLBIDX    = new TLBIDX
  val TLBEHI    = new TLBEHI
  val TLBELO0   = new TLBELO0
  val TLBELO1   = new TLBELO1
  val ASID      = new ASID
  val PGDL      = new PGDL
  val PGDH      = new PGDH
  val PGD       = new PGD
  val CPUID     = new CPUID
  val SAVE0     = new SAVE0
  val SAVE1     = new SAVE1
  val SAVE2     = new SAVE2
  val SAVE3     = new SAVE3
  val TID       = new TID
  val TCFG      = new TCFG
  val TVAL      = new TVAL
  val TICLR     = new TICLR
  val LLBCTL    = new LLBCTL
  val TLBRENTRY = new TLBRENTRY
  val CTAG      = new CTAG
  val DMW0      = new DMW0
  val DMW1      = new DMW1

  val csrlist = Seq(
    CRMD,
    PRMD,
    EUEN,
    ECFG,
    ESTAT,
    ERA,
    BADV,
    EENTRY,
    TLBIDX,
    TLBEHI,
    TLBELO0,
    TLBELO1,
    ASID,
    PGDL,
    PGDH,
    PGD,
    CPUID,
    SAVE0,
    SAVE1,
    SAVE2,
    SAVE3,
    TID,
    TCFG,
    TVAL,
    TICLR,
    LLBCTL,
    TLBRENTRY,
    CTAG,
    DMW0,
    DMW1,
  )

  // 读 or 写
  io.csrRead.data := 0.U
  for (x <- csrlist) {
    when(io.csrRead.addr === x.id) {
      io.csrRead.data := x.info.asUInt
      when(x.id === CSRCodes.TICLR) {
        io.csrRead.data := 0.U
      }
      when(x.id === CSRCodes.PGD) {
        when(!BADV.info.vaddr(ADDR_WIDTH - 1).asBool) {
          io.csrRead.data := PGDL.info.asUInt
        }.otherwise {
          io.csrRead.data := PGDH.info.asUInt
        }
      }
    }
  }

  val conuter_run = WireDefault(true.B)
  when(io.csrWrite.we) {
    for (x <- csrlist) {
      when(io.csrWrite.waddr === x.id) {
        val wdata = writeMask(io.csrWrite.wmask, x.info.asUInt, io.csrWrite.wdata)
        x.write(wdata)
        // 清除中断位 当有写1的行为
        when(x.id === CSRCodes.TICLR && wdata(0) === 1.U) {
          ESTAT.info.is_11 := false.B
        }
        when(x.id === CSRCodes.TCFG) {
          conuter_run       := false.B
          TVAL.info.timeval := wdata(COUNT_N - 1, 2) ## 3.U(2.W)
          // TVAL.info.timeval := wdata(COUNT_N - 1, 6) ## 63.U(6.W)
        }
        when(x.id === CSRCodes.LLBCTL && wdata(1)) {
          LLBCTL.info.rollb := false.B
        }
      }
    }
  }

  ASID.info.asidbits := 10.U(8.W) // stupid setting for rubbish chisel bug

  // tlb: tlbrd
  val tlbe = io.tlb.tlbe
  when(io.tlb.tlbwe) {
    switch(io.tlb.opType) {
      is(TlbOpType.rd) {
        when(tlbe.e) {
          TLBEHI.info.vppn := tlbe.vppn
          TLBELO0.info.plv := tlbe.plv(0)
          TLBELO0.info.mat := tlbe.mat(0)
          TLBELO0.info.ppn := tlbe.ppn(0)
          TLBELO0.info.g   := tlbe.g
          TLBELO0.info.d   := tlbe.d(0)
          TLBELO0.info.v   := tlbe.v(0)
          TLBELO1.info.plv := tlbe.plv(1)
          TLBELO1.info.mat := tlbe.mat(1)
          TLBELO1.info.ppn := tlbe.ppn(1)
          TLBELO1.info.g   := tlbe.g
          TLBELO1.info.d   := tlbe.d(1)
          TLBELO1.info.v   := tlbe.v(1)
          TLBIDX.info.ps   := tlbe.ps
          TLBIDX.info.ne   := false.B
          ASID.info.asid   := tlbe.asid
        }.otherwise {
          TLBIDX.info.ne := true.B
          ASID.info.asid := 0.U
          TLBEHI.info    := 0.U.asTypeOf(TLBEHI.info)
          TLBELO0.info   := 0.U.asTypeOf(TLBELO0.info)
          TLBELO1.info   := 0.U.asTypeOf(TLBELO1.info)
          TLBIDX.info.ps := 0.U
        }
      }

      is(TlbOpType.srch) {
        when(io.tlb.hitted) {
          TLBIDX.info.ne    := false.B
          TLBIDX.info.index := io.tlb.hitIndex
        }.otherwise {
          TLBIDX.info.ne := true.B
        }
      }
    }
  }

  // timer
  when(TCFG.info.en) {
    when(TVAL.info.timeval === 0.U) {
      TVAL.info.timeval := Mux(TCFG.info.preiodic, TCFG.info.initval ## 3.U(2.W), 0.U)
    }.elsewhen(conuter_run) {
      TVAL.info.timeval := TVAL.info.timeval - 1.U
    }
  }

  val TVAL_edge = ShiftRegister(TVAL.info.timeval, 1)
  when(TCFG.info.en && TVAL.info.timeval === 0.U && TVAL_edge === 1.U) {
    ESTAT.info.is_11 := true.B
  }

  ESTAT.info.is_9_2 := io.ext_int

  if (!Config.ext_int_on) {
    ESTAT.info.is_9_2 := 0.U
  }

  val any_exc = Cat(ESTAT.info.is_12, ESTAT.info.is_11, ESTAT.info.is_9_2, ESTAT.info.is_1_0) &
    Cat(ECFG.info.lie_12_11, ECFG.info.lie_9_0)
  val is_tlb_exc    = ECodes.istlbException(info.excType)
  val is_tlb_refill = info.excType === ECodes.TLBR

  val start = io.excHappen.start

  io.intExc := any_exc.orR && CRMD.info.ie

  // 例外跳转
  io.excJump := WireDefault(0.U.asTypeOf(new BranchInfo))
  when(start) {
    PRMD.info.pplv := CRMD.info.plv

    PRMD.info.pie := CRMD.info.ie
    CRMD.info.plv := 0.U
    CRMD.info.ie  := 0.U
    // 中断>例外>tlb例外的优先级，不过本身的设计保证例外和tlb例外不会同时发生，且普通例外优先
    ESTAT.info.ecode := MuxCase(
      info.excType,
      List(
        ESTAT.info.is_1_0.orR -> ECodes.INT,
        ESTAT.info.is_9_2.orR -> ECodes.INT,
        ESTAT.info.is_11      -> ECodes.INT,
        ESTAT.info.is_12      -> ECodes.INT,
      ),
    )(5, 0)
    ESTAT.info.esubcode := Mux(info.excType === ECodes.ADEM, 1.U, 0.U)
    ERA.info.pc         := info.pc
    BADV.info.vaddr := MateDefault(
      info.excType,
      BADV.info.vaddr,
      List(
        ECodes.TLBR -> info.excVAddr,
        ECodes.ADEF -> info.excVAddr,
        ECodes.ADEM -> info.excVAddr,
        ECodes.ALE  -> info.excVAddr,
        ECodes.PIL  -> info.excVAddr,
        ECodes.PIS  -> info.excVAddr,
        ECodes.PIF  -> info.excVAddr,
        ECodes.PME  -> info.excVAddr,
        ECodes.PPI  -> info.excVAddr,
      ),
    )

    io.excJump.en  := true.B
    io.excJump.tar := EENTRY.info.asUInt

    when(is_tlb_exc) {
      when(is_tlb_refill) {
        CRMD.info.da   := true.B
        CRMD.info.pg   := false.B
        io.excJump.tar := TLBRENTRY.info.asUInt
      }
      TLBEHI.info.vppn := info.excVAddr(31, 13)
    }
  }

  when(io.excHappen.end) {
    CRMD.info.plv := PRMD.info.pplv
    CRMD.info.ie  := PRMD.info.pie

    when(ESTAT.info.ecode === ECodes.TLBR) {
      CRMD.info.da := false.B
      CRMD.info.pg := true.B
    }

    when(LLBCTL.info.klo) {
      LLBCTL.info.klo := false.B
    }.otherwise {
      LLBCTL.info := 0.U.asTypeOf(LLBCTL.info)
    }

    io.excJump.en  := true.B
    io.excJump.tar := ERA.info.pc
  }

  io.plv := CRMD.info.plv

  io.tlb.is_direct := CRMD.info.da && !CRMD.info.pg
  io.tlb.asid      := ASID.info
  io.tlb.crmd      := CRMD.info
  io.tlb.dmw(0)    := DMW0.info
  io.tlb.dmw(1)    := DMW1.info
  io.tlb.tlbehi    := TLBEHI.info
  io.tlb.estat     := ESTAT.info
  io.tlb.tlbelo0   := TLBELO0.info
  io.tlb.tlbelo1   := TLBELO1.info
  io.tlb.tlbidx    := TLBIDX.info

  io.llbit := LLBCTL.info.rollb
  when(io.writeLLBCTL.en) {
    LLBCTL.info.rollb := io.writeLLBCTL.wdata
  }

  if (Config.debug_on_chiplab) {
    io.csrRegs.get.coreid    := 0.U
    io.csrRegs.get.crmd      := CRMD.info.asUInt
    io.csrRegs.get.prmd      := PRMD.info.asUInt
    io.csrRegs.get.euen      := EUEN.info.asUInt
    io.csrRegs.get.ecfg      := ECFG.info.asUInt
    io.csrRegs.get.estat     := ESTAT.info.asUInt
    io.csrRegs.get.era       := ERA.info.asUInt
    io.csrRegs.get.badv      := BADV.info.asUInt
    io.csrRegs.get.eentry    := EENTRY.info.asUInt
    io.csrRegs.get.tlbidx    := TLBIDX.info.asUInt
    io.csrRegs.get.tlbehi    := TLBEHI.info.asUInt
    io.csrRegs.get.tlbelo0   := TLBELO0.info.asUInt
    io.csrRegs.get.tlbelo1   := TLBELO1.info.asUInt
    io.csrRegs.get.asid      := ASID.info.asUInt
    io.csrRegs.get.pgdl      := PGDL.info.asUInt
    io.csrRegs.get.pgdh      := PGDH.info.asUInt
    io.csrRegs.get.save0     := SAVE0.info.asUInt
    io.csrRegs.get.save1     := SAVE1.info.asUInt
    io.csrRegs.get.save2     := SAVE2.info.asUInt
    io.csrRegs.get.save3     := SAVE3.info.asUInt
    io.csrRegs.get.tid       := TID.info.asUInt
    io.csrRegs.get.tcfg      := TCFG.info.asUInt
    io.csrRegs.get.tval      := TVAL.info.asUInt
    io.csrRegs.get.ticlr     := TICLR.info.asUInt
    io.csrRegs.get.llbctl    := LLBCTL.info.asUInt
    io.csrRegs.get.tlbrentry := TLBRENTRY.info.asUInt
    io.csrRegs.get.dmw0      := DMW0.info.asUInt
    io.csrRegs.get.dmw1      := DMW1.info.asUInt
  }
}
