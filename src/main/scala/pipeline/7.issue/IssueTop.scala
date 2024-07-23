package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class AwakeInfo extends Bundle {
  val valid = Bool()
  val preg  = UInt(PREG_WIDTH.W)
}

// this IO is defined for a single issue queue,
// which is contained inside of this issue module
class IssueQueueIO extends SingleStageBundle {
  val awake     = Input(Vec(AWAKE_NUM, new AwakeInfo))
  val busy      = Input(Vec(PREG_NUM, Bool()))
  val stall     = Input(Bool())
  val arithSize = Output(UInt((ARITH_QUEUE_WIDTH + 1).W))
}

class IssueTopIO extends Bundle {
  val flush         = Input(Bool())
  val from          = Vec(BACK_ISSUE_WIDTH, Flipped(DecoupledIO(new SingleInfo)))
  val to            = Vec(BACK_ISSUE_WIDTH, DecoupledIO(new SingleInfo))
  val awake         = Vec(AWAKE_NUM, Input(new AwakeInfo))
  val memoryStall   = Input(Bool())
  val arithSize     = Vec(ARITH_ISSUE_NUM, Output(UInt((ARITH_QUEUE_WIDTH + 1).W)))
  val busyInfo      = Vec(ISSUE_WIDTH, Input(new BusyRegUpdateInfo))
  val committedBusy = Input(new BusyRegUpdateInfo)
}

class IssueTop extends Module {
  val io = IO(new IssueTopIO)

  // issue queue def
  val arith = Seq.fill(ARITH_ISSUE_NUM)(
    Module(
      new UnorderedIssue(
        entries = ARITH_QUEUE_SIZE,
        isArithmetic = true,
      ),
    ).io,
  )
  val muldiv = Module(
    new OrderedIssue(
      entries = MULDIV_QUEUE_SIZE,
      isArithmetic = false,
    ),
  ).io
  val memory = Module(
    new OrderedIssue(
      entries = MEMORY_QUEUE_SIZE,
      isArithmetic = false,
    ),
  ).io
  val queue = arith ++ Seq(muldiv, memory)

  // busy reg
  // TODO: when add additional awake info,
  // should modify the BACK_ISSUE_WIDTH here
  val awakeInfo = WireDefault(io.awake)
  for (i <- 0 until AWAKE_NUM) {
    when(io.awake(i).valid && io.awake(i).preg === 0.U) {
      awakeInfo(i).valid := false.B
    }
  }

  val busyReg      = RegInit(VecInit(Seq.fill(PREG_NUM)(false.B)))
  val committedReg = RegInit(VecInit(Seq.fill(PREG_NUM)(false.B)))
  for (i <- 0 until ISSUE_WIDTH) {
    when(io.busyInfo(i).valid) {
      busyReg(io.busyInfo(i).preg) := true.B
    }
  }
  when(io.flush) {
    busyReg := committedReg
  }
  when(io.committedBusy.valid) {
    committedReg(io.committedBusy.preg) := true.B
    busyReg(io.committedBusy.preg)      := true.B
  }
  for (i <- 0 until AWAKE_NUM) {
    when(awakeInfo(i).valid) {
      busyReg(awakeInfo(i).preg)      := false.B
      committedReg(awakeInfo(i).preg) := false.B
    }
  }

  // pipe <> queue
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    io.from(i)     <> queue(i).from
    io.to(i)       <> queue(i).to
    queue(i).busy  := busyReg
    queue(i).awake := awakeInfo
    queue(i).flush := io.flush
    if (i == MEMORY_ISSUE_ID) {
      queue(i).stall := io.memoryStall
    } else {
      queue(i).stall := DontCare
    }
  }

  // arith size
  for (i <- 0 until ARITH_ISSUE_NUM) {
    io.arithSize(i) := arith(i).arithSize
  }
}
