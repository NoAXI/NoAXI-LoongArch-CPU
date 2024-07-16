package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class WritebackTopIO extends SingleStageBundle {
  val preg    = Flipped(new PRegWriteIO)
  val rob     = Flipped(new RobWriteIO)
  val forward = Flipped(new ForwardInfoIO)
}

class WritebackTop(
    special: String = "",
) extends Module {
  val io = IO(new WritebackTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

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
    mem2.rdata      := result
    mem2.addr       := info.pa
    mem2.op_type    := info.op_type
    res.rdInfo.data := mem2.data
    dontTouch(bitHit)
  }

  // writeback -> preg
  io.preg.en    := valid && res.iswf
  io.preg.index := res.rdInfo.preg
  io.preg.data  := res.rdInfo.data

  // writeback -> rob
  io.rob.valid := valid && !res.writeInfo.valid
  io.rob.index := res.robId

  io.rob.bits.done := true.B

  io.rob.bits.wen   := res.iswf
  io.rob.bits.areg  := res.rdInfo.areg
  io.rob.bits.preg  := res.rdInfo.preg
  io.rob.bits.opreg := res.opreg
  io.rob.bits.wdata := res.rdInfo.data

  // io.rob.bits.debug_using := true.B
  io.rob.bits.debug_pc    := res.pc
  io.rob.bits.exc_type    := res.exc_type
  io.rob.bits.exc_vaddr   := res.exc_vaddr
  io.rob.bits.isStore     := res.func_type === FuncType.mem && !MemOpType.isread(res.op_type)
  io.rob.bits.isPrivilege := res.func_type === FuncType.csr
  io.rob.bits.bfail       := res.realBr
  io.rob.bits.isbr        := res.func_type === FuncType.bru
  io.rob.bits.realBrDir   := res.realBrDir
  io.rob.bits.isException := res.exc_type =/= ECodes.NONE

  // TODO: eret
  io.rob.bits.isEret    := false.B
  io.rob.bits.isSyscall := false.B
  io.rob.bits.isEret    := false.B

  // writeback -> forward -> readreg
  doForward(io.forward, res, valid)
}
