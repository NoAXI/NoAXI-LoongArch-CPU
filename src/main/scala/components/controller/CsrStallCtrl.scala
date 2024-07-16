package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

import pipeline._

// set csr write inst stall
class CsrStallCtrlIO extends Bundle {
  val csrPush       = Input(Bool())  // issue push
  val csrPop        = Input(Bool())  // commit pop
  val memIssueStall = Output(Bool()) // when detect csr, set memory pipeline stall
  val flush         = Input(Bool())  // commit flush
}

trait CsrStallCtrlStateConst {
  val sIdle :: sWorking :: Nil = Enum(2)
}

class CsrStallCtrl extends Module with CsrStallCtrlStateConst {
  val io    = IO(new CsrStallCtrlIO)
  val state = RegInit(sIdle)
  switch(state) {
    is(sIdle) {
      when(io.csrPush && !io.flush) {
        state := sWorking
      }
    }
    is(sWorking) {
      when(io.csrPop || io.flush) {
        state := sIdle
      }
    }
  }
  io.memIssueStall := state === sWorking
}
