package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class OrderedIssue[T <: Data](
    entries: Int,
) extends Module {
  val io = IO(new IssueQueueIO)

  // basic def
  val mem     = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(new SingleInfo))))
  val headPtr = Counter(entries)
  val tailPtr = Counter(entries)

  // push / pop signal
  val doPush = WireDefault(io.from.fire)
  val doPop  = WireDefault(io.to.fire)

  // full check
  val ptrMatch  = headPtr.value === tailPtr.value
  val maybeFull = RegInit(false.B)
  val full      = maybeFull && ptrMatch
  val empty     = !maybeFull && ptrMatch

  // ptr update
  when(doPush) {
    mem(tailPtr.value) := io.from.bits
    tailPtr.inc()
  }
  when(doPop) {
    headPtr.inc()
  }
  when(doPush =/= doPop) {
    maybeFull := doPush
  }
  when(io.flush) {
    headPtr.reset()
    tailPtr.reset()
    maybeFull := false.B
  }

  // handshake
  val sendInfo = io.to.bits
  val realHit = checkIssueHit(
    sendInfo.rjInfo.preg,
    sendInfo.rkInfo.preg,
    io.awake,
    io.busy,
  )
  io.from.ready := !full
  io.to.valid   := !empty && realHit.reduce(_ && _)
  io.to.bits    := mem(headPtr.value)
}
