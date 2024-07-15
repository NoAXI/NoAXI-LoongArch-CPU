package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Mem0Mem1ForwardIO extends Bundle {
  val actualStore = Output(Bool())
  val addr        = Output(UInt(ADDR_WIDTH.W))
  val data        = Output(UInt(DATA_WIDTH.W))
  val strb        = Output(UInt((DATA_WIDTH / 8).W))
}

class Memory0TopIO extends SingleStageBundle {
  val tlb    = new Stage0TLBIO
  val dCache = new Mem0DCacheIO
  val mem1   = new Mem0Mem1ForwardIO
}

class Memory0Top extends Module {
  val io   = IO(new Memory0TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush && !raw._1.actualStore)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)

  io.mem1.actualStore := info.actualStore
  io.mem1.addr        := info.writeInfo.requestInfo.addr
  io.mem1.data        := info.writeInfo.requestInfo.wdata
  io.mem1.strb        := info.writeInfo.requestInfo.wstrb

  val va = Mux(info.actualStore, info.writeInfo.requestInfo.addr, info.rjInfo.data + info.imm)

  io.dCache.addr := va

  io.tlb.va      := va
  io.tlb.memType := Mux(MemOpType.isread(info.op_type), memType.load, memType.store)
  val hitVec   = io.tlb.hitVec
  val isDirect = io.tlb.isDirect
  val directpa = io.tlb.directpa

  res.hitVec   := hitVec
  res.isDirect := isDirect
  res.pa       := directpa
  flushWhen(raw._1, io.flush && !info.actualStore)
  io.to.bits := res

  if (Config.debug_on) {
    dontTouch(info.rjInfo.data)
    dontTouch(info.rkInfo.data)
    dontTouch(info.rdInfo.data)
  }
}
