package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Memory0TopIO extends SingleStageBundle {
  val tlb = new Stage0TLBIO
}

class Memory0Top extends Module {
  val io   = IO(new Memory0TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)

  val va = Mux(info.actualStore, info.writeInfo.requestInfo.addr, info.rjInfo.data + info.imm)

  io.tlb.va       := va
  io.tlb.memType  := Mux(MemOpType.isread(info.op_type) || info.op_type === MemOpType.cacop, memType.load, memType.store)
  io.tlb.unitType := true.B
  val hitVec = io.tlb.hitVec

  res.va     := va
  res.hitVec := hitVec
  io.to.bits := res

  if (Config.debug_on) {
    dontTouch(info.rjInfo.data)
    dontTouch(info.rkInfo.data)
    dontTouch(info.rdInfo.data)
  }
}
