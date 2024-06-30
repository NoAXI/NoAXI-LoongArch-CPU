package pipeline

import chisel3._
import chisel3.util._

import const.Parameters._
import bundles._

class ROBIO extends Bundle {
  val rename = Vec(
    ISSUE_WIDTH,
    new Bundle {
      val valid = Input(Bool())
      val index = Output(UInt(ROB_WIDTH.W))
    },
  )
  val full = Output(Bool()) // <> rename
  val write = Vec(
    BACK_ISSUE_WIDTH,
    new Bundle {
      val valid = Input(Bool())
      val index = Input(UInt(ROB_WIDTH.W))
      val bits  = Input(new ROBInfo)
    },
  )
  val commit = Vec(
    ISSUE_WIDTH,
    new Bundle {
      val valid = Output(Bool())
      val info  = Output(new ROBInfo)
    },
  )
}
class ROB extends Module {
  val io = IO(new ROBIO)

  val rob = RegInit(VecInit(Seq.fill(ROB_NUM)(0.U.asTypeOf(new ROBInfo))))

  // write info from the last stage of backend pipeline
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    val info = io.write(i)
    when(info.valid) {
      rob(info.index) := info.bits
    }
  }

  // [head, tail)
  val headPtr    = RegInit(0.U(ROB_WIDTH.W)) // inc when pop
  val tailPtr    = RegInit(0.U(ROB_WIDTH.W)) // inc when push
  val headOffset = WireDefault(0.U(2.W))
  val tailOffset = WireDefault(0.U(2.W))
  val fifoSize   = RegInit(0.U(ROB_WIDTH.W))

  // rename: push
  for (i <- 0 until ISSUE_WIDTH) {
    io.rename(i).index := 0.U
  }
  when(io.rename(0).valid && io.rename(1).valid) {
    tailOffset := 2.U
    for (i <- 0 until ISSUE_WIDTH) {
      io.rename(i).index := tailPtr + i.U
    }
  }.elsewhen(io.rename(0).valid || io.rename(1).valid) {
    tailOffset := 1.U
    for (i <- 0 until ISSUE_WIDTH) {
      when(io.rename(i).valid) {
        io.rename(i).index := tailPtr
      }
    }
  }

  // commit: pop
  val isInFifo = Vec(2, Bool())
  for (i <- 0 until ISSUE_WIDTH) {
    isInFifo(i)        := fifoSize >= (i + 1).U
    io.commit(i).info  := rob(headPtr + i.U)
    io.commit(i).valid := rob(headPtr + i.U).done && isInFifo(i)
  }
  when(io.commit(0).valid) {
    when(io.commit(1).valid) {
      headOffset := 2.U
    }.otherwise {
      headOffset := 1.U
    }
  }

  // fifo update
  fifoSize := fifoSize - headOffset + tailOffset
  headPtr  := headPtr + headOffset
  tailPtr  := tailPtr + tailOffset
}
