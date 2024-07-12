package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import utils._

class RobIO extends Bundle {
  val flush       = Input(Bool())
  val rename      = Vec(ISSUE_WIDTH, new RobRenameIO)
  val commit      = Vec(ISSUE_WIDTH, new RobCommitIO)
  val write       = Vec(BACK_ISSUE_WIDTH, new RobWriteIO)
  val renameStall = Output(Bool())
}
class Rob extends Module {
  val io = IO(new RobIO)

  val rob = Module(
    new MultiPortFifo(
      ROB_NUM,
      new RobInfo,
      hasWritePort = true,
      writePortNum = BACK_ISSUE_WIDTH,
      clearWhenPop = true,
    ),
  ).io
  rob.flush := io.flush

  // rename
  io.renameStall := WireDefault(false.B)
  for (i <- 0 until ISSUE_WIDTH) {
    rob.push(i).valid := io.rename(i).valid
    val info = WireDefault(0.U.asTypeOf(new RobInfo))
    if (Config.debug_on) {
      info.debug_pc := io.rename(i).debug_pc
    }
    rob.push(i).bits   := info
    io.rename(i).index := rob.index + i.U
    when(rob.push(i).valid && !rob.push(i).ready) {
      io.renameStall := true.B
    }
  }

  // commit
  for (i <- 0 until ISSUE_WIDTH) {
    rob.pop(i) <> io.commit(i).info
  }

  // write
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    rob.write(i) <> io.write(i)
  }
}

/*

// this is same with MultiPortBuffer

val mem       = RegInit(VecInit(Seq.fill(ROB_NUM)(0.U.asTypeOf(new RobInfo))))
val pushPtr   = RegInit(0.U(ROB_WIDTH.W))
val popPtr    = RegInit(0.U(ROB_WIDTH.W))
val maybeFull = RegInit(false.B)
val full      = maybeFull && pushPtr === popPtr
val empty     = !maybeFull && pushPtr === popPtr

val maxPush    = popPtr - pushPtr
val maxPop     = pushPtr - popPtr
val pushOffset = WireDefault(0.U(2.W))
val popOffset  = WireDefault(0.U(2.W))
val pushStall  = WireDefault(false.B)
val popStall   = WireDefault(false.B)

// gen push & pop offset
when(io.rename(0).valid && io.rename(1).valid) {
  pushOffset := 2.U
}.elsewhen(io.rename(0).valid) {
  pushOffset := 1.U
}
when(io.commit(0).ready && io.commit(1).ready) {
  popOffset := 2.U
}.elsewhen(io.commit(0).ready) {
  popOffset := 1.U
}

// when maxPush, maxPop = 0, do special judge
when(!empty && pushOffset > maxPush) {
  pushStall := true.B
}
when(!full && popOffset > maxPop) {
  popStall := true.B
}

// check if push really happened
for (i <- 0 until ISSUE_WIDTH) {
  val incSignal = (i.U < maxPush || empty) && !pushStall && !io.renameStall
  when(incSignal) {
    mem(pushPtr + i.U) := 0.U
  }
}

for (i <- 0 until ISSUE_WIDTH) {
  io.commit(i).info  := mem(popPtr + i.U)
  io.commit(i).valid := (i.U < maxPop || full) && !popStall
}
when(!pushStall) { pushPtr := pushPtr + pushOffset }
when(!popStall) { popPtr := popPtr + popOffset }
when(pushOffset =/= popOffset) {
  maybeFull := pushOffset > popOffset
}

when(io.flush) {
  pushPtr := 0.U
  popPtr  := 0.U
}

// write info from the last stage of backend pipeline
for (i <- 0 until BACK_ISSUE_WIDTH) {
  val info = io.write(i)
  when(info.valid) {
    mem(info.index) := info.bits
  }
}

// rob judge
for (i <- 0 until BACK_ISSUE_WIDTH) {
  when(io.write(i).valid) {
    mem(io.write(i).index) := io.write(i).bits
  }
}

 */
