package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class RatIO extends Bundle {
  val recover = new Bundle {
    val valid = Input(Bool())
  }
  val renameStall = Output(Bool())
  val rename = Vec(
    ISSUE_WIDTH,
    new Bundle {
      val valid = Input(Bool())
      val areg  = Input(UInt(AREG_WIDTH.W))
      val preg  = Output(UInt(PREG_WIDTH.W))
      val opreg = Output(UInt(PREG_WIDTH.W))
    },
  )
  val read = Vec(
    ISSUE_WIDTH,
    new Bundle {
      val areg = Input(Vec(2, UInt(AREG_WIDTH.W)))
      val preg = Output(Vec(2, UInt(PREG_WIDTH.W)))
    },
  )
  val commit = Vec(
    ISSUE_WIDTH,
    new Bundle {
      val valid = Input(Bool())
      val areg  = Input(UInt(AREG_WIDTH.W))
      val preg  = Input(UInt(PREG_WIDTH.W))
      val opreg = Input(UInt(PREG_WIDTH.W))
    },
  )
}

class Rat extends Module {
  val io = IO(new RatIO)

  // stall when freelist is empty (freelistSize < readSize)
  val stall = WireDefault(false.B)
  io.renameStall := stall

  // rat def
  val sRat = RegInit(VecInit(Seq.tabulate(AREG_NUM)(i => i.U(PREG_WIDTH.W))))
  val aRat = RegInit(VecInit(Seq.tabulate(AREG_NUM)(i => i.U(PREG_WIDTH.W))))

  // output read info
  // j goes until 2 cuz there's only rj & rk
  for (i <- 0 until ISSUE_WIDTH) {
    for (j <- 0 until 2) {
      val areg = io.read(i).areg(j)
      val preg = io.read(i).preg(j)
      preg := sRat(areg)
    }
  }

  // rename: sRat update
  // commit: aRat update
  when(!stall) {
    for (i <- 0 until ISSUE_WIDTH) {
      val info = io.rename(i)
      when(info.valid) {
        sRat(info.areg) := info.preg
      }
    }
  }
  when(!io.recover.valid) {
    for (i <- 0 until ISSUE_WIDTH) {
      val info = io.commit(i)
      when(info.valid) {
        aRat(info.areg) := info.preg
      }
    }
  }

  // freelist def with ptr
  // [head, tail)
  val freelist   = RegInit(VecInit(Seq.tabulate(AREG_NUM)(i => (i + AREG_NUM).U(PREG_WIDTH.W))))
  val headPtr    = RegInit(0.U(FREELIST_WIDTH.W)) // inc when pop
  val tailPtr    = RegInit(0.U(FREELIST_WIDTH.W)) // inc when push
  val headOffset = WireDefault(0.U(2.W))
  val tailOffset = WireDefault(0.U(2.W))
  val fifoSize   = RegInit(FREELIST_NUM.U(FREELIST_WIDTH.W))
  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rename(i)
    info.preg  := 0.U
    info.opreg := sRat(info.areg)
  }

  // rename: pop, head inc
  // todo: maybe should adapt to more issue width?
  when(io.rename(0).valid && io.rename(1).valid) {
    when(fifoSize < 2.U) {
      stall := true.B
    }.otherwise {
      headOffset := 2.U
      for (i <- 0 until ISSUE_WIDTH) {
        io.rename(i).preg := freelist(headPtr + i.U)
      }
    }
  }.elsewhen(io.rename(0).valid || io.rename(1).valid) {
    when(fifoSize < 1.U) {
      stall := true.B
    }.otherwise {
      headOffset := 1.U
      for (i <- 0 until ISSUE_WIDTH) {
        when(io.rename(i).valid) {
          io.rename(i).preg := freelist(headPtr)
        }
      }
    }
  }

  // commit: push, tail inc
  when(io.commit(0).valid && io.commit(1).valid) {
    tailOffset := 2.U
    for (i <- 0 until ISSUE_WIDTH) {
      freelist(tailPtr + i.U) := io.commit(i).opreg
    }
  }.elsewhen(io.commit(0).valid || io.commit(1).valid) {
    tailOffset := 1.U
    for (i <- 0 until ISSUE_WIDTH) {
      when(io.commit(i).valid) {
        freelist(tailPtr) := io.commit(i).opreg
      }
    }
  }

  // when recover, set freelist full
  when(io.recover.valid) {
    tailPtr  := headPtr
    fifoSize := FREELIST_NUM.U
    sRat     := aRat
  }.otherwise {
    fifoSize := fifoSize - headOffset + tailOffset
    headPtr  := headPtr + headOffset
    tailPtr  := tailPtr + tailOffset
  }
}
