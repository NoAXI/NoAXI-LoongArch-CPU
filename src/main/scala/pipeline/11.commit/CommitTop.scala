package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import controller._

import isa._

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
  val stallInfo     = Output(new BranchInfo)
  val stallType     = Output(StallType())

  // exception
  val excHappen    = Output(new ExcHappenInfo)
  val excJump      = Input(new BranchInfo)
  val csrWritePop  = Output(Bool())
  val tlbBufferPop = Output(Bool())

  // csr llbctl
  val writeLLBCTL = Output(new Bundle {
    val en    = Bool()
    val wdata = Bool()
  })

  // debug info output
  val debug = Vec(ISSUE_WIDTH, new DebugIO)

  val debug_chiplab = if (Config.debug_on_chiplab) Some(Output(Vec(ISSUE_WIDTH, new RobCommitBundle))) else None
  val debug_isExc   = if (Config.debug_on_chiplab) Some(Output(Bool())) else None
  val debug_rbDone  = Input(Bool())
  val debug_rbData  = Input(UInt(DATA_WIDTH.W))
}

class CommitTop extends Module {
  val io = IO(new CommitTopIO)

  // generate ready signal
  val readyBit = WireDefault(VecInit(Seq.tabulate(ISSUE_WIDTH)(i => io.rob(i).info.bits.done)))
  val doStore  = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits

    // when previous info isn't ready, set current inst stall
    if (i != 0) {
      when(!readyBit(i - 1)) {
        readyBit(i) := false.B
      }
    }

    // when detect write / csr / brfail, set next inst stall
    when(info.done && (info.isPrivilege || info.isStore || info.isbr || info.isException || info.isStall)) {
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

  if (Config.debug_on_chiplab) {
    readyBit(1)        := false.B // FOR CHIPLAB!
    io.debug_isExc.get := io.rob(0).info.bits.isException
  }

  // when got flushed or detect exception,
  // then this inst shouldn't be committed
  val excFlushReg    = RegInit(false.B)
  val excFlushSignal = WireDefault(false.B)
  excFlushReg := excFlushSignal
  when(io.flush || io.stall || excFlushReg) {
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
  // val storeCount = RegInit(0.U(3.W))
  // when(io.bufferPopValid) {
  //   storeCount := storeCount + 1.U
  // }
  // if (Config.debug_on_chiplab) {
  //   when(io.rob(0).info.ready && io.rob(0).info.bits.debug_func_type === FuncType.alu_imm && storeCount === 7.U) {
  //     io.flushInfo.en  := true.B
  //     io.flushInfo.tar := io.rob(0).info.bits.pc + 4.U
  //   }
  // }
  for (i <- 0 until ISSUE_WIDTH) {
    val info    = io.rob(i).info.bits
    val isBfail = info.bfail.en && info.isbr
    when(io.rob(i).info.ready && (isBfail || info.isPrivilege || info.isTlb)) {
      io.flushInfo.en  := true.B
      io.flushInfo.tar := info.bfail.tar
    }
  }
  when(io.excJump.en) {
    io.flushInfo := io.excJump
  }

  // stall, should make sure that stall insts are marked as priv
  io.stallInfo := 0.U.asTypeOf(io.stallInfo)
  io.stallType := 0.U
  when(io.rob(0).info.ready) {
    val info = io.rob(0).info.bits
    when(info.isStall && !info.isException) {
      io.stallInfo.en  := true.B
      io.stallInfo.tar := info.bfail.tar
      io.stallType     := info.stallType
    }
  }

  // send info
  io.debug_chiplab.get := 0.U.asTypeOf(io.debug_chiplab.get)
  for (i <- 0 until ISSUE_WIDTH) {
    val rob        = io.rob(i).info
    val writeValid = rob.fire && rob.bits.wen && !rob.bits.isException

    // rob -> commit
    io.debug(i).wb_rf_we    := Fill(4, writeValid)
    io.debug(i).wb_pc       := rob.bits.pc
    io.debug(i).wb_rf_wnum  := rob.bits.areg
    io.debug(i).wb_rf_wdata := rob.bits.wdata

    if (Config.debug_on_chiplab) {
      when(rob.fire) {
        io.debug_chiplab.get(i) := rob.bits.commitBundle
      }.otherwise {
        io.debug_chiplab.get(i) := 0.U.asTypeOf(new RobCommitBundle)
      }

      // io.debug_chiplab.get(i).DifftestInstrCommit.index := i.U
      // io.debug_chiplab.get(i).DifftestStoreEvent.index  := i.U
      // io.debug_chiplab.get(i).DifftestLoadEvent.index   := i.U
    }

    // commit -> rat
    io.rat(i).valid := writeValid
    io.rat(i).areg  := rob.bits.areg
    io.rat(i).preg  := rob.bits.preg
    io.rat(i).opreg := rob.bits.opreg

    // commit -> store buffer <> wb buffer
    doStore(i) := readyBit(i) && rob.bits.isStore && !rob.bits.isException
  }

  // store buffer
  val writeHappen = doStore.reduce(_ || _)
  io.bufferPopValid := writeHappen

  // val writeStall = writeHappen && !io.bufferToReady
  for (i <- 0 until ISSUE_WIDTH) {
    io.rob(i).info.ready := readyBit(i) // && !writeStall
  }

  // exception & csr
  val excHappen = 0.U.asTypeOf(io.excHappen)
  val excReg    = RegNext(excHappen)
  io.excHappen := excReg
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(io.rob(i).info.ready && info.isException) {
      when(info.exc_type === ECodes.ertn) {
        excHappen.end := true.B
      }.otherwise {
        excHappen.start := true.B
      }
      excFlushSignal          := true.B
      excHappen.info.excType  := info.exc_type
      excHappen.info.excVAddr := info.exc_vaddr
      excHappen.info.pc       := info.pc
      excHappen.info.pc_add_4 := info.pc + 4.U
    }
  }
  val csrCurPop = WireDefault(false.B)
  val csrPopReg = RegNext(csrCurPop)
  io.csrWritePop := csrPopReg
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(io.rob(i).info.ready && info.csr_iswf) {
      csrCurPop := true.B
    }
  }

  // FOR CHIPLAB!
  io.writeLLBCTL.en := (io.rob(0).info.bits.commitBundle.DifftestLoadEvent.valid(5)
    || io.rob(0).info.bits.commitBundle.DifftestStoreEvent.valid(3)) && io
    .rob(0)
    .info
    .fire && !io.rob(0).info.bits.isException
  io.writeLLBCTL.wdata := io.rob(0).info.bits.commitBundle.DifftestLoadEvent.valid(5) // ll

  // tlb
  val tlbCurPop = WireDefault(false.B)
  val tlbPopReg = RegNext(tlbCurPop)
  io.tlbBufferPop := tlbPopReg
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rob(i).info.bits
    when(io.rob(i).info.ready && info.isTlb) {
      tlbCurPop := true.B
    }
  }

  // FOR CHIPLAB
  if (Config.debug_on_chiplab) {
    val stall       = RegInit(false.B)
    val unldInfoReg = RegInit(0.U.asTypeOf(new RobInfo))
    val stallSignal = io.rob(0).info.fire && io.rob(0).info.bits.debug_isUncachedLoad && !io.rob(0).info.bits.isException
    when(stallSignal) {

      stall       := true.B
      unldInfoReg := io.rob(0).info.bits
      io.debug(0) := 0.U.asTypeOf(io.debug(0))

      io.debug_chiplab.get(0) := 0.U.asTypeOf(io.debug_chiplab.get(0))
    }
    when(io.debug_rbDone) {
      stall                   := false.B
      io.debug(0).wb_rf_we    := unldInfoReg.wen
      io.debug(0).wb_pc       := unldInfoReg.pc
      io.debug(0).wb_rf_wnum  := unldInfoReg.areg
      io.debug(0).wb_rf_wdata := io.debug_rbData

      io.debug_chiplab.get(0)                           := unldInfoReg.commitBundle
      io.debug_chiplab.get(0).DifftestInstrCommit.wdata := io.debug_rbData
    }
    when(stall) {
      readyBit := 0.U.asTypeOf(readyBit)
    }
  } else {
    io.debug_rbDone := DontCare
  }
}
