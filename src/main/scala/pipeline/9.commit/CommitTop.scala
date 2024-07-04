package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

// this stage isn't linked with any other stage
// only for rob retire
class CommitTopIO extends Bundle {
  val rob   = Flipped(Vec(ISSUE_WIDTH, new RobCommitIO))
  val rat   = Flipped(Vec(ISSUE_WIDTH, new RatCommitIO))
  val debug = Vec(ISSUE_WIDTH, new DebugIO)
}

class CommitTop extends Module {
  val io = IO(new CommitTopIO)
  for (i <- 0 until ISSUE_WIDTH) {
    val robInfo  = io.rob(i).info
    val robValid = io.rob(i).valid
    val valid    = robValid && robInfo.wen

    // rob -> commit
    io.debug(i).wb_rf_we    := valid
    io.debug(i).wb_pc       := robInfo.debug_pc
    io.debug(i).wb_rf_wnum  := robInfo.areg
    io.debug(i).wb_rf_wdata := robInfo.wdata

    // commit -> rat
    io.rat(i).valid := valid
    io.rat(i).areg  := robInfo.areg
    io.rat(i).preg  := robInfo.preg
    io.rat(i).opreg := robInfo.opreg
  }
}
