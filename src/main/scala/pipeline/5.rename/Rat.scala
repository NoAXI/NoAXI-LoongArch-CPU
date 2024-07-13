package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class RatRenameIO extends Bundle {
  val valid = Input(Bool())
  val areg  = Input(UInt(AREG_WIDTH.W))
  val preg  = Output(UInt(PREG_WIDTH.W))
  val opreg = Output(UInt(PREG_WIDTH.W))
}

class RatReadIO extends Bundle {
  val areg = Input(new Bundle {
    val rj = UInt(AREG_WIDTH.W)
    val rk = UInt(AREG_WIDTH.W)
  })
  val preg = Output(new Bundle {
    val rj = UInt(PREG_WIDTH.W)
    val rk = UInt(PREG_WIDTH.W)
  })
}

class RatCommitIO extends Bundle {
  val valid = Input(Bool())
  val areg  = Input(UInt(AREG_WIDTH.W))
  val preg  = Input(UInt(PREG_WIDTH.W))
  val opreg = Input(UInt(PREG_WIDTH.W))
}

class RatIO extends Bundle {
  val flush  = Input(Bool())
  val empty  = Output(Bool()) // <> rename
  val rename = Vec(ISSUE_WIDTH, new RatRenameIO)
  val read   = Vec(ISSUE_WIDTH, new RatReadIO)
  val commit = Vec(ISSUE_WIDTH, new RatCommitIO)
}

class Rat extends Module {
  val io = IO(new RatIO)

  // rat def
  val sRat = RegInit(VecInit(Seq.tabulate(AREG_NUM)(i => i.U(PREG_WIDTH.W))))
  val aRat = RegInit(VecInit(Seq.tabulate(AREG_NUM)(i => i.U(PREG_WIDTH.W))))

  // output read info
  for (i <- 0 until ISSUE_WIDTH) {
    io.read(i).preg.rj := sRat(io.read(i).areg.rj)
    io.read(i).preg.rk := sRat(io.read(i).areg.rk)
  }

  // rename: sRat update
  when(!io.empty) {
    for (i <- 0 until ISSUE_WIDTH) {
      val info = io.rename(i)
      when(info.valid && info.areg =/= 0.U) {
        sRat(info.areg) := info.preg
      }
    }
  }
  // commit: aRat update
  when(!io.flush) {
    for (i <- 0 until ISSUE_WIDTH) {
      val info = io.commit(i)
      when(info.valid && info.areg =/= 0.U) {
        aRat(info.areg) := info.preg
      }
    }
  }

  // freelist def with ptr
  // [head, tail)
  val freelist  = RegInit(VecInit(Seq.tabulate(AREG_NUM)(i => (i + AREG_NUM).U(PREG_WIDTH.W))))
  val pushPtr   = RegInit(0.U(AREG_WIDTH.W))
  val popPtr    = RegInit(0.U(AREG_WIDTH.W))
  val maybeFull = RegInit(true.B)
  val full      = maybeFull && pushPtr === popPtr
  val empty     = !maybeFull && pushPtr === popPtr

  val maxPush    = popPtr - pushPtr
  val maxPop     = pushPtr - popPtr
  val pushOffset = WireDefault(0.U(2.W))
  val popOffset  = WireDefault(0.U(2.W))
  val pushStall  = WireDefault(false.B)
  val popStall   = WireDefault(false.B)

  for (i <- 0 until ISSUE_WIDTH) {
    val info = io.rename(i)
    info.preg  := 0.U
    info.opreg := sRat(info.areg)
  }

  // rename: pop, head inc
  when(io.rename(0).valid && io.rename(1).valid) {
    popOffset := 2.U
  }.elsewhen(io.rename(0).valid || io.rename(1).valid) {
    popOffset := 1.U
  }
  for (i <- 0 until ISSUE_WIDTH) {
    when(popOffset === 2.U) {
      io.rename(i).preg := freelist(popPtr + i.U)
    }.elsewhen(popOffset === 1.U) {
      io.rename(i).preg := freelist(popPtr)
    }
  }

  // commit: push, tail inc
  when(io.commit(0).valid && io.commit(1).valid) {
    pushOffset := 2.U
  }.elsewhen(io.commit(0).valid || io.commit(1).valid) {
    pushOffset := 1.U
  }
  for (i <- 0 until ISSUE_WIDTH) {
    when(pushOffset === 2.U) {
      freelist(pushPtr + i.U) := io.commit(i).opreg
    }.elsewhen(pushOffset === 1.U && io.commit(i).valid) {
      freelist(pushPtr) := io.commit(i).opreg
    }
  }

  when(!empty && pushOffset > maxPush) {
    pushStall := true.B
  }
  when(!full && popOffset > maxPop) {
    popStall := true.B
  }
  io.empty := popStall

  // when recover, set freelist full
  when(io.flush) {
    pushPtr   := popPtr
    maybeFull := true.B
    sRat      := aRat
  }.otherwise {
    when(!pushStall) { pushPtr := pushPtr + pushOffset }
    when(!popStall) { popPtr := popPtr + popOffset }
    val realPushOffset = Mux(pushStall, 0.U, pushOffset)
    val realPopOffset  = Mux(popStall, 0.U, popOffset)
    when(realPushOffset =/= realPopOffset) {
      maybeFull := realPushOffset > realPopOffset
    }
  }
}

/*

discard code
using fifoSize

// freelist def with ptr
// [head, tail)
val freelist   = RegInit(VecInit(Seq.tabulate(AREG_NUM)(i => (i + AREG_NUM).U(PREG_WIDTH.W))))
val headPtr    = RegInit(0.U(FREELIST_WIDTH.W)) // inc when pop
val tailPtr    = RegInit(0.U(FREELIST_WIDTH.W)) // inc when push
val headOffset = WireDefault(0.U(2.W))
val tailOffset = WireDefault(0.U(2.W))
val fifoSize   = RegInit(FREELIST_NUM.U((FREELIST_WIDTH + 1).W))
for (i <- 0 until ISSUE_WIDTH) {
  val info = io.rename(i)
  info.preg  := 0.U
  info.opreg := sRat(info.areg)
}

// rename: pop, head inc
// TODO: maybe should adapt to more issue width?
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
when(io.flush) {
  tailPtr  := headPtr
  fifoSize := FREELIST_NUM.U
  sRat     := aRat
}.otherwise {
  fifoSize := fifoSize - headOffset + tailOffset
  headPtr  := headPtr + headOffset
  tailPtr  := tailPtr + tailOffset
}

 */
