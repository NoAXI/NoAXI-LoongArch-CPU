package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FlushCtrlIO extends Bundle {
  val flushInfo  = Input(new br)
  val fetchStall = Input(Bool())

  val frontFlush  = Output(Bool())
  val backFlush   = Output(Bool())
  val recover     = Output(Bool()) // let rob, rat flushed
  val commitStall = Output(Bool())

  val flushTarget = Output(new br)
}
class FlushCtrl extends Module {
  val io = IO(new FlushCtrlIO)

  val frontFlush = WireDefault(false.B)
  val recover    = WireDefault(false.B)

  // flush reg
  // when fetch got stall, keep flush state
  val flushReg  = RegInit(false.B)
  val flushNext = WireDefault(flushReg)
  when(!flushReg && io.flushInfo.en) {
    flushNext := true.B
  }
  when(flushReg && !io.fetchStall) {
    flushNext := false.B
  }
  flushReg   := flushNext
  frontFlush := flushReg
  recover    := flushReg

  // branch reg
  // when next clock flush signal removes, update the branch info
  val addrReg = RegInit(0.U(ADDR_WIDTH.W))
  when(!flushReg) {
    addrReg := io.flushInfo.tar
  }
  io.flushTarget.tar := addrReg
  io.flushTarget.en  := flushReg && !flushNext
  io.commitStall     := flushReg

  io.frontFlush := frontFlush
  io.backFlush  := recover
  io.recover    := recover
}
