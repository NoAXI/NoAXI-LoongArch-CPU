package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FlushCtrlIO extends Bundle {
  val hasFlush   = Input(Bool())
  val doFlush    = Input(Bool())
  val frontFlush = Output(Bool()) // flush (fetch, decode) when br = 1
  val backFlush  = Output(Bool())
}

trait FlushCtrlStateTable {
  val sIdle :: sFlushWait :: Nil = Enum(2)
}

// TODO: add flush logic here
class FlushCtrl extends Module with FlushCtrlStateTable {
  val io    = IO(new FlushCtrlIO)
  val state = RegInit(sIdle)

  val generalFlush = WireDefault(false.B)
  val frontFlush   = WireDefault(false.B)
  val backFlush    = WireDefault(false.B)
  switch(state) {
    is(sIdle) {
      when(io.hasFlush && !io.doFlush) {
        state := sFlushWait
      }
    }
    is(sFlushWait) {
      generalFlush := true.B
      when(io.doFlush) {
        state := sIdle
      }
    }
  }
  when(generalFlush) {
    frontFlush := true.B
    backFlush  := true.B
  }
  io.frontFlush := frontFlush
  io.backFlush  := backFlush
}
