package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Muldiv0TopIO extends SingleStageBundle {
  val mul = Flipped(new Mul2Mul0IO)

  // csr
  val commitCsrWriteDone = Input(Bool())
  val csrWrite           = Output(new CSRWrite)
  val csrRead            = Flipped(new CsrReadIO)

  // tlb
  val commitTlbDone = Input(Bool())
  val tlbBufferInfo = Output(new TlbBufferInfo)
}

class Muldiv0Top extends Module {
  val io = IO(new Muldiv0TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  val mul = Module(new Mul).io
  val div = Module(new Div).io

  val src1   = info.rjInfo.data
  val src2   = info.rkInfo.data
  val is_div = info.func_type === FuncType.div

  // mul
  mul.op_type   := info.op_type // for muldiv2 to get data
  mul.src1      := src1
  mul.src2      := src2
  io.mul.result := mul.result
  mul.op_type2  := io.mul.op_type

  // div
  div.running := is_div
  div.op_type := info.op_type
  div.src1    := src1
  div.src2    := src2

  // calculate csr_wmask
  val is_xchg = info.func_type === FuncType.csr && info.op_type === CsrOpType.xchg
  res.csr_wmask := Mux(is_xchg, info.rjInfo.data, Fill(32, 1.U))

  // csr read
  io.csrRead.addr := info.csr_addr
  when(FuncType.isPrivilege(info.func_type)) {
    res.rdInfo.data := io.csrRead.data
  }

  // csr buffer
  val csrOccupied   = RegInit(false.B)
  val csrWriteInfo  = RegInit(0.U.asTypeOf(new CSRWrite))
  val csrPushSignal = info.isWriteCsr && io.to.fire && valid && !info.bubble
  val csrPopSignal  = io.commitCsrWriteDone
  when(csrPushSignal) {
    csrWriteInfo.we    := res.isWriteCsr
    csrWriteInfo.wmask := res.csr_wmask
    csrWriteInfo.waddr := res.csr_addr
    csrWriteInfo.wdata := res.rkInfo.data
  }
  when(csrPushSignal =/= csrPopSignal) {
    csrOccupied := csrPushSignal
  }
  io.csrWrite := Mux(io.commitCsrWriteDone, csrWriteInfo, 0.U.asTypeOf(io.csrWrite))
  when(io.flush) {
    csrOccupied := false.B
  }

  // tlb buffer
  val isTlb         = info.func_type === FuncType.tlb
  val tlbOccupied   = RegInit(false.B)
  val tlbBufferInfo = RegInit(0.U.asTypeOf(new TlbBufferInfo))
  val tlbPushSignal = isTlb && io.to.fire && valid && !info.bubble
  val tlbPopSignal  = io.commitTlbDone
  when(tlbPushSignal) {
    tlbBufferInfo.en       := isTlb
    tlbBufferInfo.op       := info.rdInfo.areg
    tlbBufferInfo.inv.asid := info.rjInfo.data(9, 0)
    tlbBufferInfo.inv.va   := info.rkInfo.data
    tlbBufferInfo.opType   := info.op_type
  }
  when(tlbPushSignal =/= tlbPopSignal) {
    tlbOccupied := tlbPushSignal
  }
  io.tlbBufferInfo := Mux(io.commitTlbDone, tlbBufferInfo, 0.U.asTypeOf(io.tlbBufferInfo))
  when(io.flush) {
    tlbOccupied := false.B
  }

  // when has div / csr / tlb, set stall
  busy := (div.running && !div.complete) || (csrOccupied && info.isReadCsr) || (tlbOccupied && isTlb)

  // avoid multi-request
  val div_complete = RegInit(false.B)
  if (Config.debug_on) {
    dontTouch(div_complete)
  }
  when(is_div && div.complete) {
    div_complete := true.B
  }
  when(div_complete || div.complete) {
    div.running := false.B
    busy        := false.B
  }
  when(io.from.fire) {
    div_complete := false.B
  }

  when(info.func_type === FuncType.div) {
    res.rdInfo.data := div.result
  }
}
