package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FlushCtrlIO extends Bundle {
  val hasFlush     = Input(Bool())
  val doFlush      = Input(Bool())
  val generalFlush = Output(Bool())
  val frontFlush   = Output(Bool()) // flush (fetch, decode) when br = 1
}

trait FlushCtrlStateTable {
  val sIdle :: sFlushWait :: Nil = Enum(2)
}

class FlushCtrl extends Module with FlushCtrlStateTable {
  val io    = IO(new FlushCtrlIO)
  val state = RegInit(sIdle)
  io.generalFlush := false.B
  io.frontFlush   := false.B
  switch(state) {
    is(sIdle) {
      when(io.hasFlush && !io.doFlush) {
        state := sFlushWait
      }
    }
    is(sFlushWait) {
      io.generalFlush := true.B
      when(io.doFlush) {
        state := sIdle
      }
    }
  }
}
