package utils

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class MultiPortFifoWriteInfo[T <: Data](
    entries: Int,
    gen: T,
) extends Bundle {
  val valid = Input(Bool())
  val index = Input(UInt(log2Ceil(entries).W))
  val bits  = Input(gen)
}

class MultiPortFifo[T <: Data](
    entries: Int,
    gen: T,
    hasWritePort: Boolean = false,
    writePortNum: Int = 0,
    forIB: Boolean = false,
) extends Module {
  require(isPow2(entries))
  require((!hasWritePort && writePortNum == 0) || (hasWritePort && writePortNum != 0))

  val io = IO(new Bundle {
    val push  = Vec(ISSUE_WIDTH, Flipped(Decoupled(gen)))
    val pop   = Vec(ISSUE_WIDTH, Decoupled(gen))
    val write = Vec(writePortNum, new MultiPortFifoWriteInfo(entries, gen))
    val flush = Input(Bool())
  })
  val mem       = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(gen))))
  val pushPtr   = RegInit(0.U(log2Ceil(entries).W))
  val popPtr    = RegInit(0.U(log2Ceil(entries).W))
  val maybeFull = RegInit(false.B)
  val full      = maybeFull && pushPtr === popPtr
  val empty     = !maybeFull && pushPtr === popPtr

  val maxPush    = popPtr - pushPtr
  val maxPop     = pushPtr - popPtr
  val pushOffset = WireDefault(0.U(2.W))
  val popOffset  = WireDefault(0.U(2.W))
  val pushStall  = WireDefault(false.B)
  val popStall   = WireDefault(false.B)

  when(io.push(0).valid && io.push(1).valid) {
    pushOffset := 2.U
  }.elsewhen(io.push(0).valid) {
    pushOffset := 1.U
  }
  when(io.pop(0).ready && io.pop(1).ready) {
    popOffset := 2.U
  }.otherwise {
    if (forIB) {
      popOffset := 0.U
    } else {
      popOffset := io.pop(0).ready
    }
  }

  when(!empty && pushOffset > maxPush) {
    pushStall := true.B
  }
  when(!full && popOffset > maxPop) {
    popStall := true.B
  }

  for (i <- 0 until ISSUE_WIDTH) {
    when(i.U < pushOffset && !pushStall) {
      mem(pushPtr + i.U) := io.push(i).bits
    }
    io.push(i).ready := (i.U < maxPush || empty) && !pushStall
  }

  for (i <- 0 until ISSUE_WIDTH) {
    // when(i.U < popOffset && !popStall) {
    //   mem(popPtr + i.U) := 0.U.asTypeOf(mem(popPtr + i.U))
    // }
    io.pop(i).bits  := mem(popPtr + i.U)
    io.pop(i).valid := (i.U < maxPop || full) && !popStall
  }
  when(!pushStall) { pushPtr := pushPtr + pushOffset }
  when(!popStall) { popPtr := popPtr + popOffset }
  when(pushOffset =/= popOffset) {
    maybeFull := pushOffset > popOffset
  }

  when(io.flush) {
    pushPtr := 0.U
    popPtr  := 0.U
  }

  if (hasWritePort) {
    for (i <- 0 until BACK_ISSUE_WIDTH) {
      when(io.write(i).valid) {
        mem(io.write(i).index) := io.write(i).bits
      }
    }
  } else {
    io.write := DontCare
  }

  if (Config.debug_on) {
    dontTouch(pushOffset)
    dontTouch(popOffset)
    dontTouch(full)
    dontTouch(empty)
    dontTouch(maxPush)
    dontTouch(maxPop)
  }
}