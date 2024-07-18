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
  val recover     = Output(Bool()) // let rob, rat flushed
  val commitStall = Output(Bool())

  val flushTarget = Output(new BranchInfo)
}
class FlushCtrl extends Module {
  val io = IO(new FlushCtrlIO)

  // val frontFlush = WireDefault(false.B)
  // val recover    = WireDefault(false.B)

  // flush reg
  // when fetch got stall, keep flush state
  // val flushReg = RegInit(false.B)
  // // val backFlush = RegInit(false.B)
  // val flushNext = WireDefault(flushReg)
  // when(!flushReg && io.flushInfo.en) {
  //   flushNext := true.B
  //   // backFlush := true.B
  // }
  // when(flushReg && !io.fetchStall) {
  //   flushNext := false.B
  // }
  // // when(backFlush) {
  // //   backFlush := false.B
  // // }
  // flushReg   := flushNext
  // frontFlush := flushReg
  // recover    := flushReg

  // // branch reg
  // // when next clock flush signal removes, update the branch info
  // val addrReg = RegInit(0.U(ADDR_WIDTH.W))
  // when(!flushReg) {
  //   addrReg := io.flushInfo.tar
  // }

  val doFlush    = io.flushInfo.en
  val delayFlush = RegNext(doFlush)

  io.flushTarget := io.flushInfo
  io.commitStall := false.B

  io.frontFlush := doFlush
  io.backFlush  := doFlush || delayFlush
  io.recover    := delayFlush
}
