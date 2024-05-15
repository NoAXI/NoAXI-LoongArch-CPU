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
  val counter = Output(UInt(64.W))

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

  val stable_counter = RegInit(0.U(64.W))
  stable_counter := Mux(stable_counter === Fill(2, ALL_MASK.U), 0.U, stable_counter + 1.U)
  io.counter := stable_counter

  // 读 or 写
  io.rdata := 0.U
  when(io.re) {
    for (x <- csrlist) {
      when(io.raddr === x.id) {
        io.rdata := x.info.asUInt
        when(x.id === CSRCodes.TICLR) {
          io.rdata := 0.U
        }
      }
    }
  }


  when(io.rf_bus.we) {
    for (x <- csrlist) {
      when(io.rf_bus.waddr === x.id) {
        val wdata = writeMask(io.rf_bus.wmask, x.info.asUInt, io.rf_bus.wdata)
        x.write(wdata)
        // 清除中断位 当有写1的行为
        when(x.id === CSRCodes.TICLR && wdata(0) === 1.U) {
          ESTAT.info.is_11 := false.B
        }
        when(x.id === CSRCodes.TCFG) {
          TVAL.info.timeval := wdata(COUNT_N - 1, 2) ## 1.U(2.W) + 10.U
        }
      }
    }
  }

  when (TCFG.info.en) {
    when (TVAL.info.timeval === 0.U) {
      TVAL.info.timeval := Mux(TCFG.info.preiodic, TCFG.info.initval ## 1.U(2.W) + 10.U, 0.U)
    }.otherwise {
      TVAL.info.timeval := TVAL.info.timeval - 1.U
    }
  }

  val TVAL_edge = ShiftRegister(TVAL.info.timeval, 1)
  when(TCFG.info.en && TVAL.info.timeval === 0.U && TVAL_edge === 1.U){
    ESTAT.info.is_11 := true.B
  }

  val any_exc = Cat(ESTAT.info.is_12, ESTAT.info.is_11, ESTAT.info.is_9_2, ESTAT.info.is_1_0) &
                Cat(ECFG.info.lie_12_11, ECFG.info.lie_9_0) 
  val start = io.start || (any_exc.orR && CRMD.info.ie)

  // 例外跳转
  io.exc_bus := WireDefault(0.U.asTypeOf(new exc_bus))
  when(start) {
    PRMD.info.pplv := CRMD.info.plv
    PRMD.info.pie  := CRMD.info.ie
    CRMD.info.plv  := 0.U
    CRMD.info.ie   := 0.U
    // 中断>例外的优先级
    ESTAT.info.ecode := MuxCase(
      io.info.exc_type(6, 0),
      List(
        ESTAT.info.is_1_0.orR -> ECodes.INT,
        ESTAT.info.is_9_2.orR -> ECodes.INT,
        ESTAT.info.is_11 -> ECodes.INT,
        ESTAT.info.is_12 -> ECodes.INT,
      ),
    )
    ESTAT.info.esubcode := Mux(io.info.ecode === ECodes.ADEM, 1.U, 0.U)
    ERA.info.pc := Mux(ESTAT.info.is_1_0.orR, io.info.pc + 4.U, io.info.pc) // tm的如果说是软中断，它记录的一定是下个pc，下个pc一定也是+4
    BADV.info.vaddr := MateDefault(
      io.info.exc_type,
      BADV.info.vaddr,
      List(
        ECodes.ADEF -> io.info.wrong_addr,
        ECodes.ADEM -> io.info.wrong_addr,
        ECodes.ALE  -> io.info.wrong_addr,
      ),
    )

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
