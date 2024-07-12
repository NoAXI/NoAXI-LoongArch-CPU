package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class CommitTopIO extends Bundle {
  val rob = Flipped(Vec(ISSUE_WIDTH, new RobCommitIO))
  val rat = Flipped(Vec(ISSUE_WIDTH, new RatCommitIO))
  val buffer = new Bundle {
    val from = Flipped(DecoupledIO(new BufferInfo))
    val to   = DecoupledIO(new BufferInfo)
  }
  // val flush = new Bundle {
  // val info    = Output(new RobInfo)
  // }
  val doFlush = Output(Bool())
  val bres    = Output(new PredictRes)
  val debug   = Vec(ISSUE_WIDTH, new DebugIO)
}

class CommitTop extends Module {
  val io = IO(new CommitTopIO)

  // generate ready signal
  val flushSignal = WireDefault(false.B)
  val hasFlush = Seq.tabulate(ISSUE_WIDTH) { i =>
    val rob = io.rob(i).info
    rob.bits.bfail.en || rob.bits.exc_type =/= ECodes.NONE
  }
  val readyBit = WireDefault(VecInit(Seq.tabulate(ISSUE_WIDTH)(i => io.rob(i).info.bits.done)))
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info

    // when previous info isn't ready, set current inst stall
    if (i != 0) {
      when(!readyBit(i - 1)) {
        readyBit(i) := false.B
      }
    }
    when(info.bits.done && info.bits.isStore) {
      for (j <- i + 1 until ISSUE_WIDTH) {
        readyBit(j) := false.B
      }
    }
    // when detect write / csr / brfail, set next inst stall
    when(info.bits.done && (info.bits.isPrivilege || hasFlush(i))) {
      for (j <- i until ISSUE_WIDTH) {
        readyBit(j) := false.B
      }
      when(hasFlush(i)) {
        flushSignal := true.B
      }
    }
  }

  // send fulsh signal
  io.doFlush := false.B
  io.bres    := 0.U.asTypeOf(new PredictRes)
  when(flushSignal) {
    for (i <- 0 until ISSUE_WIDTH) {
      // use real ready here to avoid unexpected flush
      when(io.rob(i).info.ready && hasFlush(i)) {
        io.doFlush := true.B
        val info = io.rob(i).info.bits
        io.bres.br            := info.bfail
        io.bres.realDirection := info.realBrDir
        io.bres.pc            := info.debug_pc
      }
    }
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
    io.debug(i).wb_rf_wnum  := rob.bits.areg
    io.debug(i).wb_rf_wdata := rob.bits.wdata

    // commit -> rat
    io.rat(i).valid := writeValid
    io.rat(i).areg  := rob.bits.areg
    io.rat(i).preg  := rob.bits.preg
    io.rat(i).opreg := rob.bits.opreg

    // commit -> store buffer <> wb buffer
    doStore(i) := readyBit(i) && rob.bits.isStore
  }

  val writeHappen = doStore.reduce(_ || _)
  io.buffer.to.bits    := io.buffer.from.bits
  io.buffer.to.valid   := io.buffer.from.valid && writeHappen
  io.buffer.from.ready := io.buffer.to.ready && writeHappen
  when(writeHappen && !io.buffer.to.ready) {
    writeStall := true.B
  }

  for (i <- 0 until ISSUE_WIDTH) {
    io.rob(i).info.ready := readyBit(i) && !writeStall
  }
}
