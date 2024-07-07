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

  // flush ctrl when exception appears in memory pipeline
  val robFlush      = Output(Bool())
  val ratFlush      = Output(Bool())
  val issueMemStall = Output(Bool())
  val memFlush      = Vec(MEMORY_STAGE_NUM, Output(Bool()))
}

trait FlushCtrlStateTable {
  val sIdle :: sFlushWait :: Nil = Enum(2)
}

// TODO: add flush logic here
class FlushCtrl extends Module with FlushCtrlStateTable {
  val io    = IO(new FlushCtrlIO)
  val state = RegInit(sIdle)

  val generalFlush  = WireDefault(false.B)
  val frontFlush    = WireDefault(false.B)
  val backFlush     = WireDefault(false.B)
  val robFlush      = WireDefault(false.B)
  val ratFlush      = WireDefault(false.B)
  val issueMemStall = WireDefault(false.B)
  val memFlush      = WireDefault(VecInit(Seq.fill(MEMORY_STAGE_NUM)(false.B)))
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
  io.frontFlush    := frontFlush
  io.backFlush     := backFlush
  io.robFlush      := robFlush
  io.ratFlush      := ratFlush
  io.issueMemStall := issueMemStall
  for (i <- 0 until MEMORY_STAGE_NUM) {
    io.memFlush(i) := memFlush(i)
  }
}
