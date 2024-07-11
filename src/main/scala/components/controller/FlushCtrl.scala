package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class FlushCtrlIO extends Bundle {
  val hasFlush = Input(Bool()) // from mem1: there's a should-do-flush inst in pipeline
  val doFlush  = Input(Bool()) // from rob: the should-do-flush inst has been committed

  val frontFlush = Output(Bool()) // flush all frontend info
  val backFlush  = Output(Bool()) // only flush back when recover
  val recover    = Output(Bool()) // let rob, rat flushed
  val memStall   = Output(Bool()) // set (readreg -> mem0) stall, let mem0 flushed
  val ibStall    = Output(Bool()) // set (ib -> decode) stall
}

trait FlushCtrlStateTable  { val sIdle :: sFlushWait :: Nil = Enum(2)                   }
trait FlushCtrlMemoryConst { val mStageRR :: mStageM0 :: mStageM1 :: mStageM2 = Enum(4) }
object FlushCtrlConst extends FlushCtrlStateTable with FlushCtrlMemoryConst
import FlushCtrlConst._

class FlushCtrl extends Module {
  val io    = IO(new FlushCtrlIO)
  val state = RegInit(sIdle)

  val frontFlush = WireDefault(false.B)
  val recover    = WireDefault(false.B)
  val memStall   = WireDefault(false.B)
  val ibStall    = WireDefault(false.B)

  // FSM
  switch(state) {
    is(sIdle) {
      when(io.doFlush) {
        frontFlush := true.B
        recover    := true.B
      }.elsewhen(io.hasFlush) {
        memStall   := true.B
        frontFlush := true.B
        state      := sFlushWait
      }
    }
    is(sFlushWait) {
      memStall := true.B
      ibStall  := true.B
      when(io.doFlush) {
        state   := sIdle
        recover := true.B
      }
    }
  }

  io.frontFlush := frontFlush
  io.backFlush  := recover
  io.recover    := recover
  io.memStall   := memStall
  io.ibStall    := ibStall
}
