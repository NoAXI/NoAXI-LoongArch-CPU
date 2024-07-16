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
  val flushInfo     = Output(new br)

  // exception
  val excInfo   = Output(new ExceptionInfo)
  val excHappen = Output(new ExcHappenInfo)
  val csrWrite  = Output(new CSRWrite)
  val excJump   = Input(new br)

  // debug info output
  val debug = Vec(ISSUE_WIDTH, new DebugIO)
}

class CommitTop extends Module {
  val io = IO(new CommitTopIO)

  // generate ready signal
  val readyBit = WireDefault(VecInit(Seq.tabulate(ISSUE_WIDTH)(i => io.rob(i).info.bits.done)))
  val hasEx    = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
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
      if (i == 1) {
        when(info.isStore) {
          readyBit(i) := false.B
        }
      }
      for (j <- i + 1 until ISSUE_WIDTH) {
        readyBit(j) := false.B
      }
      when(info.isException) {
        hasEx(i) := true.B
      }
    }
  }

  // when got flushed or detect exception,
  // then this inst shouldn't be committed
  when(io.flush || io.stall || hasEx.reduce(_ || _)) {
    readyBit := 0.U.asTypeOf(readyBit)
  }

  // send predict update info
  io.predictResult := 0.U.asTypeOf(new PredictRes)
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(io.rob(i).info.ready && info.isbr) {
      val out = io.predictResult
      out.br            := info.bfail
      out.realDirection := info.realBrDir
      out.pc            := info.debug_pc
      out.isbr          := info.isbr
    }
  }

  // when ex / bfail appears, do flush
  io.flushInfo := 0.U.asTypeOf(new br)
  for (i <- 0 until ISSUE_WIDTH) {
    val info    = io.rob(i).info.bits
    val isBfail = info.bfail.en && info.isbr
    val isEx    = info.isException
    // TODO: add exception here
    when(io.rob(i).info.ready && isBfail) {
      io.flushInfo.en  := true.B
      io.flushInfo.tar := info.bfail.tar
    }
  }
  when(io.excJump.en) {
    io.flushInfo := io.excJump
  }

  // send info
  val writeStall = WireDefault(false.B)
  val doStore    = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
  for (i <- 0 until ISSUE_WIDTH) {
    val rob        = io.rob(i).info
    val writeValid = rob.fire && rob.bits.wen

    // rob -> commit
    io.debug(i).wb_rf_we    := writeValid
    io.debug(i).wb_pc       := Mux(writeValid, rob.bits.debug_pc, 0.U)
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
  when(writeHappen && !io.bufferToReady) {
    writeStall := true.B
  }
  io.bufferPopValid := writeHappen

  for (i <- 0 until ISSUE_WIDTH) {
    io.rob(i).info.ready := readyBit(i) && !writeStall
  }

  // exception
  io.excInfo := 0.U.asTypeOf(io.excInfo)
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(hasEx(i)) {
      io.excInfo.en       := true.B
      io.excInfo.excType  := info.exc_type
      io.excInfo.excVAddr := info.exc_vaddr
    }
  }
  io.excHappen := 0.U.asTypeOf(io.excHappen)
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(info.isSyscall || info.isEret) {
      io.excHappen.start := info.isSyscall
      io.excHappen.end   := info.isEret
    }
  }
  io.csrWrite := 0.U.asTypeOf(io.csrWrite)
}
