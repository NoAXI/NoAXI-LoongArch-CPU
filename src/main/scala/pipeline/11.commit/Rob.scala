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
      clearWhenFlush = true,
    ),
  ).io
  rob.flush := io.flush

  // rename
  io.renameStall := WireDefault(false.B)
  for (i <- 0 until ISSUE_WIDTH) {
    rob.push(i).valid := io.rename(i).valid
    val info = WireDefault(0.U.asTypeOf(new RobInfo))
    if (Config.debug_on) {
      info.pc := io.rename(i).debug_pc
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
