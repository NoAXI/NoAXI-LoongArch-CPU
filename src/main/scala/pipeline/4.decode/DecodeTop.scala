package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.PipelineType

class DecodeTopIO extends StageBundle {
  val intExc = Input(Bool())
}

class DecodeTop extends Module {
  val io   = IO(new DecodeTopIO)
  val busy = WireDefault(false.B)
  val from = stageConnect(io.from, io.to, busy, io.flush)

  val info = from._1

  val dec     = VecInit.fill(ISSUE_WIDTH)(Module(new Decoder).io)
  val to_info = WireDefault(info)

  for (i <- 0 until ISSUE_WIDTH) {
    dec(i).pc   := info.bits(i).pc
    dec(i).inst := info.bits(i).inst

    to_info.bits(i).func_type   := dec(i).func_type
    to_info.bits(i).op_type     := dec(i).op_type
    to_info.bits(i).imm         := dec(i).imm
    to_info.bits(i).src1IsZero  := dec(i).src1IsZero
    to_info.bits(i).src1Ispc    := dec(i).src1Ispc
    to_info.bits(i).src2IsFour  := dec(i).src2IsFour
    to_info.bits(i).src2IsImm   := dec(i).src2IsImm
    to_info.bits(i).rjInfo.areg := dec(i).rj
    to_info.bits(i).rkInfo.areg := dec(i).rk
    to_info.bits(i).rdInfo.areg := dec(i).rd
    to_info.bits(i).iswf        := dec(i).iswf && !info.bits(i).bubble
    to_info.bits(i).isReadCsr   := dec(i).isReadCsr
    to_info.bits(i).isWriteCsr  := dec(i).isWriteCsr
    to_info.bits(i).csr_addr    := dec(i).csrReg
    to_info.bits(i).isCALL      := dec(i).isCALL
    to_info.bits(i).isReturn    := dec(i).isReturn
    to_info.bits(i).exc_type := MuxCase(
      dec(i).exc_type,
      Seq(
        io.intExc                               -> ECodes.INT,
        (info.bits(i).exc_type =/= ECodes.NONE) -> info.bits(i).exc_type,
      ),
    )
    to_info.bits(i).exc_vaddr    := info.bits(i).pc
    to_info.bits(i).pipelineType := Mux(info.bits(i).bubble, PipelineType.nop, dec(i).pipelineType)
  }

  io.to.bits := to_info
}
