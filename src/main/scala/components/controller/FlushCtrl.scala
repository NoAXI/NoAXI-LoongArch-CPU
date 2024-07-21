package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FlushCtrlIO extends Bundle {
  val flushInfo = Input(new BranchInfo)

  val frontFlush  = Output(Bool())
  val backFlush   = Output(Bool())
  val recover     = Output(Bool()) // let rob, rat flushed
  val commitStall = Output(Bool())

  val flushTarget = Output(new BranchInfo)
}
class FlushCtrl extends Module {
  val io = IO(new FlushCtrlIO)

  val doFlush    = io.flushInfo.en
  val delayFlush = RegNext(doFlush)

  io.flushTarget := RegNext(io.flushInfo)
  io.commitStall := false.B

  io.frontFlush := delayFlush
  io.backFlush  := delayFlush
  io.recover    := delayFlush
}
