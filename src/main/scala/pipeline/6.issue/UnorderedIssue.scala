package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class UnorderedIssue[T <: Data](
    entries: Int,
    isArithmetic: Boolean,
) extends Module {
  val io = IO(new IssueQueueIO)

  val mem       = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(new SingleInfo))))
  val topPtr    = RegInit(0.U(log2Ceil(entries).W))
  val incSignal = WireDefault(false.B)
  val decSignal = WireDefault(false.B)
  val maybeFull = RegInit(false.B)
  val full      = maybeFull && topPtr === 0.U
  val empty     = !maybeFull && topPtr === 0.U

  // hit check
  val hitVec = WireDefault(VecInit(Seq.fill(entries)(false.B)))
  for (i <- 0 until entries) {
    for (j <- 0 until OPERAND_MAX) {
      val regHit = checkIssueHit(
        mem(i).rjInfo.preg,
        mem(i).rkInfo.preg,
        io.awake,
        io.busy,
      )
      hitVec(i) := regHit.reduce(_ && _)
    }
  }

  // info shifting
  val shiftVec = WireDefault(VecInit(Seq.fill(entries)(false.B)))
  for (i <- 0 until entries) {
    if (i > 0) {
      shiftVec(i) := shiftVec(i - 1) | hitVec(i)
    } else {
      shiftVec(i) := hitVec(i)
    }
  }
  for (i <- 0 until entries) {
    when(shiftVec(i)) {
      if (i < (entries - 1)) {
        mem(i) := mem(i + 1)
      } else {
        mem(i) := 0.U.asTypeOf(mem(i))
      }
    }
  }

  // ptr update
  when(io.from.fire) {
    mem(topPtr) := io.from.bits
  }
  when(incSignal =/= decSignal) {
    when(incSignal) {
      topPtr    := topPtr + 1.U
      maybeFull := true.B
    }.otherwise {
      topPtr    := topPtr - 1.U
      maybeFull := false.B
    }
  }
  when(io.flush) {
    topPtr    := 0.U
    maybeFull := false.B
  }

  // handshake
  io.from.ready := !full
  io.to.valid   := hitVec.reduce(_ || _) && !empty
  val index = PriorityEncoder(hitVec)
  io.to.bits := mem(index)

  // get size
  if (isArithmetic) {
    io.arithSize := Mux(full, entries.U, topPtr)
  } else {
    io.arithSize := DontCare
  }
}
