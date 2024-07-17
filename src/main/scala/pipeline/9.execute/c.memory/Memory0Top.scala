package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Memory0TopIO extends SingleStageBundle {
  val tlb     = new Stage0TLBIO
  val csrRead = Flipped(new CsrReadIO)
}

class Memory0Top extends Module {
  val io   = IO(new Memory0TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush && !raw._1.actualStore)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)

  // calculate csr_wmask
  val is_xchg = info.func_type === FuncType.csr && info.op_type === CsrOpType.xchg
  res.csr_wmask := Mux(is_xchg, info.rjInfo.data, ALL_MASK.U)
  
  // csr read
  io.csrRead.addr := info.csr_addr
  res.rdInfo.data := io.csrRead.data

  // csr hazard
  // val csrWriteCount = RegInit(0.U(ROB_WIDTH.W))
  // when(res.func_type === FuncType.csr && res.op_type === CsrOpType.)

  val va = Mux(info.actualStore, info.writeInfo.requestInfo.addr, info.rjInfo.data + info.imm)

  io.tlb.va      := va
  io.tlb.memType := Mux(MemOpType.isread(info.op_type), memType.load, memType.store)
  val hitVec   = io.tlb.hitVec
  val isDirect = io.tlb.isDirect
  val directpa = io.tlb.directpa

  res.va       := va
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
