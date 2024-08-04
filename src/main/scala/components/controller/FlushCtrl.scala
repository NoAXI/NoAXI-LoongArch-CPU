package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FlushCtrlIO extends Bundle {
  val flushInfo    = Input(new BranchInfo)
  val stallRecover = Input(new BranchInfo)

  val frontFlush  = Output(Bool())
  val backFlush   = Output(Bool())
  val recover     = Output(Bool()) // let rob, rat flushed
  val commitStall = Output(Bool())

  val flushTarget = Output(new BranchInfo)
}
class FlushCtrl extends Module {
  val io = IO(new FlushCtrlIO)

  val flushReg = RegInit(0.U.asTypeOf(io.flushInfo))
  when(io.stallRecover.en) {
    flushReg.en  := true.B
    flushReg.tar := io.stallRecover.tar
  }.elsewhen(io.flushInfo.en) {
    flushReg.en  := true.B
    flushReg.tar := io.flushInfo.tar
  }.otherwise {
    flushReg := 0.U.asTypeOf(flushReg)
  }
  io.flushTarget := flushReg

  io.commitStall := false.B
  io.frontFlush  := flushReg.en
  io.backFlush   := flushReg.en
  io.recover     := flushReg.en
}
