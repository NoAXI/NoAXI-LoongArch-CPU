package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class RenameTopIO extends StageBundle {
  // rat
  val ratRename = Flipped(Vec(ISSUE_WIDTH, new RatRenameIO))
  val ratRead   = Flipped(Vec(ISSUE_WIDTH, new RatReadIO))
  val ratFull   = Input(Bool())

  // rob
  val rob     = Flipped(Vec(ISSUE_WIDTH, new RobRenameIO))
  val robFull = Input(Bool())
}

class RenameTop extends Module {
  val io = IO(new RenameTopIO)

  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = io.to.fire && raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  when(io.ratFull || io.robFull) {
    busy.info(0) := true.B
  }

  for (i <- 0 until ISSUE_WIDTH) {
    val from = info.bits(i)
    val to   = res.bits(i)

    // rename -> rat
    io.ratRename(i).valid := valid
    io.ratRename(i).areg  := from.rdInfo.areg
    io.ratRead(i).areg.rj := to.rjInfo.areg
    io.ratRead(i).areg.rk := to.rkInfo.areg

    // rat -> rename
    to.opreg       := io.ratRename(i).opreg
    to.rdInfo.preg := io.ratRename(i).preg
    to.rjInfo.preg := io.ratRead(i).preg.rj
    to.rkInfo.preg := io.ratRead(i).preg.rk

    // rob
    io.rob(i).valid := valid
    to.robId        := io.rob(i).index
  }
}