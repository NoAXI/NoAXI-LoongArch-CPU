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
  val awake     = Input(Vec(BACK_ISSUE_WIDTH, new AwakeInfo))
  val busy      = Input(Vec(PREG_NUM, Bool()))
  val stall     = Input(Bool())
  val arithSize = Output(UInt((ARITH_QUEUE_WIDTH + 1).W))
}

class IssueTopIO extends Bundle {
  val flush = Input(Bool())
  val from  = Vec(BACK_ISSUE_WIDTH, Flipped(DecoupledIO(new SingleInfo)))
  val to    = Vec(BACK_ISSUE_WIDTH, DecoupledIO(new SingleInfo))
  val awake = Vec(BACK_ISSUE_WIDTH, Input(new AwakeInfo))
  val stall = Vec(BACK_ISSUE_WIDTH, Input(Bool()))
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
  val busyReg = RegInit(VecInit(Seq.fill(PREG_NUM)(false.B)))
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    when(io.awake(i).valid) {
      busyReg(io.awake(i).preg) := false.B
    }
  }
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    when(io.to(i).fire) {
      busyReg(io.to(i).bits.rdInfo.preg) := true.B
    }
  }

  // pipe <> queue
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    io.from(i)     <> queue(i).from
    io.to(i)       <> queue(i).to
    queue(i).busy  := busyReg
    queue(i).awake := io.awake
    queue(i).stall := io.stall(i)
    queue(i).flush := io.flush
  }
}
