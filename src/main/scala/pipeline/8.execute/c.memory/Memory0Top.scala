package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.MemOpType

// this pipeline contains of following inst
// memory access inst
// branch check inst
class Memory0TopIO extends SingleStageBundle {
  val dCache = new Mem0DCacheIO
  val tlb    = new Mem0TLBIO
}

class Memory0Top extends Module {
  val io = IO(new Memory0TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2

  val hasExc = info.exc_type =/= ECodes.NONE

  val vaddr = info.rjInfo.data + info.imm

  io.dCache.request.valid     := io.from.fire
  io.dCache.request.bits.addr := vaddr // VI

  io.tlb.va       := vaddr
  io.tlb.mem_type := Mux(MemOpType.isread(info.op_type), memType.load, memType.store)

  io.to.bits           := info
  io.to.bits.pa        := io.tlb.pa
  io.to.bits.cached    := io.tlb.cached
  io.to.bits.exc_type  := Mux(hasExc, info.exc_type, io.tlb.exc_type)
  io.to.bits.exc_vaddr := Mux(hasExc, info.exc_vaddr, io.tlb.exc_vaddr)
}
