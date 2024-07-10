package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Memory0TopIO extends SingleStageBundle {
  val tlb    = new Stage0TLBIO
  val dCache = new Mem0DCacheIO
}

class Memory0Top extends Module {
  val io = IO(new Memory0TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  // mem0.va -> dcache.tagSram
  io.dCache.request.valid     := io.from.fire
  io.dCache.request.bits.addr := info.va

  // mem0.va -> tlb
  io.tlb.va      := info.va
  io.tlb.memType := Mux(MemOpType.isread(info.op_type), memType.load, memType.store)

  // tlb.hitVec -> mem0
  res.hitVec   := io.tlb.hitVec
  res.isDirect := io.tlb.isDirect
}
