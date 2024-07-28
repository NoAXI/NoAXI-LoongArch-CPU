package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class CommitTopIO extends Bundle {
  // architectural state update
  val rob = Flipped(Vec(ISSUE_WIDTH, new RobCommitIO))
  val rat = Flipped(Vec(ISSUE_WIDTH, new RatCommitIO))

  // store buffer (pop port) connect
  val bufferPopValid = Output(Bool())
  val bufferToReady  = Input(Bool())

  // ctrl signal
  val flush         = Input(Bool())
  val stall         = Input(Bool())
  val predictResult = Output(new PredictRes)
  val flushInfo     = Output(new BranchInfo)

  // exception
  val excHappen   = Output(new ExcHappenInfo)
  val excJump     = Input(new BranchInfo)
  val csrWritePop = Output(Bool())

  // debug info output
  val debug = Vec(ISSUE_WIDTH, new DebugIO)
}

class CommitTop extends Module {
  val io = IO(new CommitTopIO)

  // generate ready signal
  val readyBit = WireDefault(VecInit(Seq.tabulate(ISSUE_WIDTH)(i => io.rob(i).info.bits.done)))
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits

    // when previous info isn't ready, set current inst stall
    if (i != 0) {
      when(!readyBit(i - 1)) {
        readyBit(i) := false.B
      }
    }

    // when detect write / csr / brfail, set next inst stall
    when(info.done && (info.isPrivilege || info.isStore || info.isbr || info.isException)) {
      if (i != 0) {
        when(info.isStore || info.isException || info.isPrivilege) {
          readyBit(i) := false.B
        }
      }
      for (j <- i + 1 until ISSUE_WIDTH) {
        readyBit(j) := false.B
      }
    }
  }

  // when got flushed or detect exception,
  // then this inst shouldn't be committed
  when(io.flush || io.stall) {
    readyBit := 0.U.asTypeOf(readyBit)
  }

  // send predict update info
  val predictResult = WireDefault(0.U.asTypeOf(new PredictRes))
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(io.rob(i).info.ready && info.isbr) {
      predictResult.br            := info.bfail
      predictResult.realDirection := info.realBrDir
      predictResult.pc            := info.pc
      predictResult.isbr          := info.isbr
      predictResult.isCALL        := info.isCALL
      predictResult.isReturn      := info.isReturn
    }
  }
  io.predictResult := RegNext(predictResult)

  // when ex / bfail appears, do flush
  io.flushInfo := 0.U.asTypeOf(new BranchInfo)
  for (i <- 0 until ISSUE_WIDTH) {
    val info    = io.rob(i).info.bits
    val isBfail = info.bfail.en && info.isbr
    when(io.rob(i).info.ready && (isBfail || info.isPrivilege)) {
      io.flushInfo.en  := true.B
      io.flushInfo.tar := info.bfail.tar
    }
  }
  when(io.excJump.en) {
    io.flushInfo := io.excJump
  }

  // send info
  val doStore = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
  for (i <- 0 until ISSUE_WIDTH) {
    val rob        = io.rob(i).info
    val writeValid = rob.fire && rob.bits.wen && !rob.bits.isException

    // rob -> commit
    io.debug(i).wb_rf_we    := writeValid
    io.debug(i).wb_pc       := Mux(writeValid, rob.bits.pc, 0.U)
    io.debug(i).wb_rf_wnum  := Mux(writeValid, rob.bits.areg, 0.U)
    io.debug(i).wb_rf_wdata := Mux(writeValid, rob.bits.wdata, 0.U)

    // commit -> rat
    io.rat(i).valid := writeValid
    io.rat(i).areg  := rob.bits.areg
    io.rat(i).preg  := rob.bits.preg
    io.rat(i).opreg := rob.bits.opreg

    // commit -> store buffer <> wb buffer
    doStore(i) := readyBit(i) && rob.bits.isStore
  }

  // store buffer
  val writeHappen = doStore.reduce(_ || _)
  io.bufferPopValid := writeHappen

  // val writeStall = writeHappen && !io.bufferToReady
  for (i <- 0 until ISSUE_WIDTH) {
    io.rob(i).info.ready := readyBit(i) // && !writeStall
  }

  // exception & csr
  io.excHappen := 0.U.asTypeOf(io.excHappen)
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(io.rob(i).info.ready && info.isException) {
      when(info.exc_type === ECodes.ertn) {
        io.excHappen.end := true.B
      }.otherwise {
        io.excHappen.start := true.B
      }
      io.excHappen.info.excType  := info.exc_type
      io.excHappen.info.excVAddr := info.exc_vaddr
      io.excHappen.info.pc       := info.pc
      io.excHappen.info.pc_add_4 := info.pc + 4.U
    }
  }
  val csrCurPop = WireDefault(false.B)
  val csrPopReg = RegInit(csrCurPop)
  io.csrWritePop := csrPopReg
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(io.rob(i).info.ready && info.csr_iswf) {
      csrCurPop := true.B
    }
  }
}