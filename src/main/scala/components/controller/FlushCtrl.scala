package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FlushCtrlIO extends Bundle {
  val flushInfo  = Input(new BranchInfo)
  val fetchStall = Input(Bool())

  val frontFlush  = Output(Bool())
  val backFlush   = Output(Bool())
  val commitFlush = Output(Bool())
  val robFlush    = Output(Bool())
  val recover     = Output(Bool()) // let rob, rat flushed

  val flushTarget = Output(new BranchInfo)
}
class FlushCtrl extends Module {
  val io = IO(new FlushCtrlIO)

  val doFlush = io.flushInfo.en
  // val doFlush    = RegNext(io.flushInfo.en)
  val delayFlush = RegNext(doFlush)

  io.flushTarget := io.flushInfo

  io.frontFlush  := doFlush
  io.backFlush   := delayFlush
  io.recover     := delayFlush
  io.robFlush    := doFlush || delayFlush
  io.commitFlush := delayFlush
}
