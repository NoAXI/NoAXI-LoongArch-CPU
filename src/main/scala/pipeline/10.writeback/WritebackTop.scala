package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._
import controller._

class WritebackTopIO extends SingleStageBundle {
  val preg    = Flipped(new PRegWriteIO)
  val rob     = Flipped(new RobWriteIO)
  val forward = Flipped(new ForwardInfoIO)
  val awake   = Output(new AwakeInfo)
  val writeLLBCTL = Output(new Bundle {
    val en    = Bool()
    val wdata = Bool()
  })
  val cacopDone = Output(Bool())
  val llDone    = Output(Bool())

  val debug_uncached = if (Config.debug_on) Some(new DebugIO) else None
}

class WritebackTop(
    special: String = "",
) extends Module {
  val io = IO(new WritebackTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy, io.flush, special == "memory")

  val info  = raw._1
  val valid = io.to.fire && raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  // load merge
  if (special == "memory") {
    val bitHit  = WireDefault(VecInit(Seq.fill(4)(0.U(8.W))))
    val bitStrb = WireDefault(VecInit(Seq.fill(4)(false.B)))
    for (i <- 0 until 2) {
      when(info.forwardHitVec(i)) {
        for (j <- 0 to 3) {
          when(info.forwardStrb(i)(j)) {
            bitStrb(j) := true.B
            bitHit(j)  := info.forwardData(i)(j * 8 + 7, j * 8)
          }
        }
      }
    }
    val bitMask = Cat((3 to 0 by -1).map(i => Fill(8, bitStrb(i))))
    val result  = writeMask(bitMask, info.ldData, bitHit.asUInt)

    val mem2 = Module(new MemoryLoadAccess).io
    mem2.rdata      := Mux(info.actualStore, info.ldData, result)
    mem2.addr       := Mux(info.actualStore, info.writeInfo.requestInfo.addr, info.pa)
    mem2.op_type    := Mux(info.actualStore, info.writeInfo.requestInfo.wstrb, info.op_type)
    res.rdInfo.data := Mux(info.actualStore && info.writeInfo.requestInfo.atom && info.writeInfo.requestInfo.rbType, res.canrequest, mem2.data)
    dontTouch(bitHit)

    // write LLBCTL
    io.writeLLBCTL.en    := info.actualStore && info.writeInfo.requestInfo.atom
    io.writeLLBCTL.wdata := !info.writeInfo.requestInfo.rbType
    io.llDone            := io.writeLLBCTL.en

    val writeStop = RegInit(false.B)
    when(io.writeLLBCTL.en) {
      writeStop := true.B
    }
    when(writeStop || info.bubble) {
      io.writeLLBCTL.en := false.B
    }
    when(io.from.fire) {
      writeStop := false.B
    }

    // judge roll-back cacop
    io.cacopDone := info.actualStore && info.writeInfo.requestInfo.cacop.en

    io.awake.valid := io.preg.en
    io.awake.preg  := info.rdInfo.preg
    doForward(io.forward, res, false.B)
  } else {
    io.awake       := DontCare
    io.writeLLBCTL := DontCare
    io.cacopDone   := DontCare
    io.llDone      := DontCare
    doForward(io.forward, res, valid)
  }

  // writeback -> preg
  io.preg.en    := valid && res.iswf
  io.preg.index := res.rdInfo.preg
  io.preg.data  := res.rdInfo.data

  // writeback -> rob
  io.rob.valid := valid && !res.writeInfo.valid && !res.bubble && !res.actualStore
  io.rob.index := res.robId

  // basic rob info
  io.rob.bits.done  := true.B
  io.rob.bits.pc    := res.pc
  io.rob.bits.wen   := res.iswf || info.uncachedLoad
  io.rob.bits.areg  := res.rdInfo.areg
  io.rob.bits.preg  := res.rdInfo.preg
  io.rob.bits.opreg := res.opreg
  io.rob.bits.wdata := res.rdInfo.data
  io.rob.bits.isStore := res.func_type === FuncType.mem && (
    MemOpType.iswrite(res.op_type)
      || (MemOpType.isread(res.op_type) && !info.cached)
      || res.op_type === MemOpType.cacop
      || MemOpType.isatom(res.op_type)
  )

  // branch
  io.rob.bits.bfail     := res.realBr
  io.rob.bits.isbr      := res.func_type === FuncType.bru
  io.rob.bits.realBrDir := res.realBrDir
  io.rob.bits.isCALL    := res.isCALL
  io.rob.bits.isReturn  := res.isReturn

  // exception & privilege
  val isExc = res.func_type === FuncType.exc
  val isPri = FuncType.isPrivilege(res.func_type) || io.rob.bits.isStall
  io.rob.bits.exc_type    := res.exc_type
  io.rob.bits.exc_vaddr   := res.exc_vaddr
  io.rob.bits.isPrivilege := isPri
  io.rob.bits.isException := res.exc_type =/= ECodes.NONE

  when(isPri) {
    io.rob.bits.bfail.tar := res.pc + 4.U
  }

  // stall
  val isIdle  = res.func_type === FuncType.alu && res.op_type === AluOpType.idle
  val isCacop = res.func_type === FuncType.mem && res.op_type === MemOpType.cacop
  val isAtom  = res.func_type === FuncType.mem && MemOpType.isatom(res.op_type)
  val stallType = MuxCase(
    3.U,
    Seq(
      isIdle  -> 0.U,
      isCacop -> 1.U,
      isAtom  -> 2.U,
    ),
  ) // idle -> 0, cacop -> 1, atom -> 2
  io.rob.bits.isStall   := isIdle || isCacop || isAtom
  io.rob.bits.stallType := stallType

  // csr / tlb
  io.rob.bits.csr_iswf := res.isWriteCsr
  io.rob.bits.isTlb    := res.func_type === FuncType.tlb

  if (Config.debug_on) {
    if (special == "memory") {
      when(info.actualStore && !info.writeInfo.requestInfo.rbType && valid) {
        io.debug_uncached.get.wb_pc       := 0.U
        io.debug_uncached.get.wb_rf_we    := true.B
        io.debug_uncached.get.wb_rf_wnum  := res.rdInfo.areg
        io.debug_uncached.get.wb_rf_wdata := res.rdInfo.data
      }.otherwise {
        io.debug_uncached.get := 0.U.asTypeOf(new DebugIO)
      }
    } else {
      io.debug_uncached.get := DontCare
    }
  }
}
