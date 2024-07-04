package pipeline

import chisel3._
import chisel3.util._

import const.Parameters._
import bundles._

class RobRenameIO extends Bundle {
  val valid = Input(Bool())
  val index = Output(UInt(ROB_WIDTH.W))
}

class RobWriteIO extends Bundle {
  val valid = Input(Bool())
  val index = Input(UInt(ROB_WIDTH.W))
  val bits  = Input(new ROBInfo)
}

class RobCommitIO extends Bundle {
  val valid = Output(Bool())
  val info  = Output(new ROBInfo)
}

class RobIO extends Bundle {
  val flush  = Input(Bool())
  val full   = Output(Bool()) // <> rename
  val rename = Vec(ISSUE_WIDTH, new RobRenameIO)
  val write  = Vec(BACK_ISSUE_WIDTH, new RobWriteIO)
  val commit = Vec(ISSUE_WIDTH, new RobCommitIO)
}
class Rob extends Module {
  val io = IO(new RobIO)

  val rob = RegInit(VecInit(Seq.fill(ROB_NUM)(0.U.asTypeOf(new ROBInfo))))

  // write info from the last stage of backend pipeline
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    val info = io.write(i)
    when(info.valid) {
      rob(info.index) := info.bits
    }
  }

  // rob full info
  val stall = WireDefault(false.B)
  io.full := stall

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
    when(fifoSize > (ROB_NUM - 2).U) {
      stall := true.B
    }.otherwise {
      tailOffset := 2.U
      for (i <- 0 until ISSUE_WIDTH) {
        io.rename(i).index := tailPtr + i.U
      }
    }
  }.elsewhen(io.rename(0).valid || io.rename(1).valid) {
    when(fifoSize > (ROB_NUM - 1).U) {
      stall := true.B
    }.otherwise {
      tailOffset := 1.U
      for (i <- 0 until ISSUE_WIDTH) {
        when(io.rename(i).valid) {
          io.rename(i).index := tailPtr
        }
      }
    }
  }

  // commit: pop
  for (i <- 0 until ISSUE_WIDTH) {
    io.commit(i).info  := rob(headPtr + i.U)
    io.commit(i).valid := rob(headPtr + i.U).done && fifoSize >= (i + 1).U
  }
  when(io.commit(0).valid) {
    when(io.commit(1).valid) {
      headOffset := 2.U
    }.otherwise {
      headOffset := 1.U
    }
  }

  // fifo update
  when(io.flush) {
    fifoSize := 0.U
    headPtr  := 0.U
    tailPtr  := 0.U
  }.elsewhen(!stall) {
    fifoSize := fifoSize - headOffset + tailOffset
    headPtr  := headPtr + headOffset
    tailPtr  := tailPtr + tailOffset
    for (i <- 0 until ISSUE_WIDTH) {
      when(i.U < tailOffset) {
        rob(tailPtr + i.U).done := false.B
      }
    }
  }
}
