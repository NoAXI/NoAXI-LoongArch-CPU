package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Memory0TopIO extends SingleStageBundle {
  val tlb                = new Stage0TLBIO
  val csrRead            = Flipped(new CsrReadIO)
  val commitCsrWriteDone = Input(Bool())
}

class Memory0Top extends Module {
  val io   = IO(new Memory0TopIO)
  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush)

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
  // TODO: tlb指令也需要在这里加入判断
  val csrWriteCount = RegInit(false.B)
  val csrPushSignal = info.isWriteCsr && io.to.fire && valid
  val csrPopSignal  = io.commitCsrWriteDone
  when(csrPushSignal =/= csrPopSignal) {
    when(csrPushSignal) {
      csrWriteCount := true.B
    }.otherwise {
      csrWriteCount := false.B
    }
  }
  when(io.flush) {
    csrWriteCount := false.B
  }
  when(csrWriteCount =/= false.B && info.isReadCsr) {
    busy := true.B
  }

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
  io.to.bits := res

  if (Config.debug_on) {
    dontTouch(info.rjInfo.data)
    dontTouch(info.rkInfo.data)
    dontTouch(info.rdInfo.data)
  }
}
