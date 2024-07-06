package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class AwakeInfo extends Bundle {
  val valid = Input(Bool())
  val preg  = Input(UInt(PREG_WIDTH.W))
}

// this IO is defined for a single issue queue,
// which is contained inside of this issue module
class IssueQueueIO extends SingleStageBundle {
  val awake = Input(Vec(BACK_ISSUE_WIDTH, new AwakeInfo))
  val busy  = Input(Vec(PREG_NUM, Bool()))
  val stall = Input(Bool())
}

class IssueTopIO extends Bundle {
  val flush = Input(Bool())
  val pipe  = Vec(BACK_ISSUE_WIDTH, new SingleStageBundleWithoutFlush)
  val awake = Vec(BACK_ISSUE_WIDTH, new AwakeInfo)
  val stall = Vec(BACK_ISSUE_WIDTH, Input(Bool()))
}

class IssueTop extends Module {
  val io = IO(new IssueTopIO)

  // issue queue def
  val arith0 = Module(new OrderedIssue(ARITH_QUEUE_SIZE)).io
  val arith1 = Module(new OrderedIssue(ARITH_QUEUE_SIZE)).io
  val muldiv = Module(new UnorderedIssue(MULDIV_QUEUE_SIZE)).io
  val memory = Module(new UnorderedIssue(MEMORY_QUEUE_SIZE)).io
  val queue  = Seq(arith0, arith1, muldiv, memory)

  // busy reg
  val busyReg = RegInit(VecInit(Seq.fill(PREG_NUM)(false.B)))
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    when(io.awake(i).valid) {
      busyReg(i) := false.B
    }
  }
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    when(io.pipe(i).to.fire) {
      busyReg(io.pipe(i).to.bits.rdInfo.preg) := true.B
    }
  }

  // pipe <> queue
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    io.pipe(i).from <> queue(i).from
    io.pipe(i).to   <> queue(i).to
    queue(i).busy   := busyReg
    queue(i).awake  := io.awake
    queue(i).stall  := io.stall(i)
    queue(i).flush  := io.flush
  }
}

/*

对于推测唤醒：
当前拍位于exe

当前拍位于writeback的指令，下一拍会正常写入preg
所以writeback返回的validbits，除了已经提交的指令
还需要or上当前writeback级正在写的那些位置

 */
