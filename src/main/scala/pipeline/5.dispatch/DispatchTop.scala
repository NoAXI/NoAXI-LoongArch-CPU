package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import isa.PipelineType

class DispatchStageBundle extends Bundle {
  val from  = Flipped(DecoupledIO(new DualInfo))
  val to    = Vec(BACK_ISSUE_WIDTH, DecoupledIO(new SingleInfo))
  val flush = Input(Bool())
}
class DispatchTopIO extends DispatchStageBundle {
  val arithSize = Input(Vec(ARITH_ISSUE_NUM, UInt(ARITH_QUEUE_WIDTH.W)))
}

class DispatchTop extends Module {
  val io = IO(new DispatchTopIO)

  // stage connect (from)
  val stall    = WireDefault(false.B)
  val info     = RegInit(0.U.asTypeOf(new DualInfo))
  val validReg = RegInit(false.B)
  val fireVec  = WireDefault(VecInit(Seq.fill(BACK_ISSUE_WIDTH)(true.B)))
  io.from.ready := !validReg || (fireVec.reduce(_ && _) && !stall)
  when(io.from.ready) {
    validReg := io.from.valid
  }
  when(io.from.fire) {
    info := io.from.bits
  }
  val defaultValid = validReg && !stall
  flushWhen(info, io.flush)

  // select issued pipeline
  val bits  = info.bits
  val cango = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
  val port  = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(0.U(log2Ceil(BACK_ISSUE_WIDTH).W))))
  when(bits(0).pipelineType === bits(1).pipelineType) {
    when(bits(0).pipelineType === PipelineType.arith) {
      for (i <- 0 until ISSUE_WIDTH) {
        cango(i) := true.B
        port(i)  := i.U
      }
    }.otherwise {
      cango(0) := true.B
      port(0)  := bits(0).pipelineType
      cango(1) := false.B
      port(1)  := 0.U
      stall    := true.B
    }
  }.otherwise {
    for (i <- 0 until ISSUE_WIDTH) {
      when(bits(i).pipelineType === PipelineType.arith) {
        port(i) := Mux(io.arithSize(0) < io.arithSize(1), 0.U, 1.U)
      }.otherwise {
        port(i) := bits(i).pipelineType
      }
    }
  }
  for (i <- 0 until ISSUE_WIDTH) {
    when(bits(i).pipelineType === PipelineType.nop) {
      cango(i) := false.B
      stall    := false.B
    }
  }

  // check if issue succeeded
  // when issue failed, set stall
  val issueFailed = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
  for (i <- 0 until ISSUE_WIDTH) {
    io.to(port(i)).valid := cango(i)
    issueFailed(i)       := cango(i) && !io.to(port(i)).fire
  }
  for (i <- 0 until ISSUE_WIDTH) {
    for (j <- 0 until BACK_ISSUE_WIDTH) {
      when(port(i) === j.U) {
        io.to(j).valid := cango(i)
      }
    }
  }
  when(issueFailed.reduce(_ || _)) {
    stall := true.B
  }

  // when stall, flush issued inst
  when(stall && !io.flush) {
    for (i <- 0 until ISSUE_WIDTH) {
      when(cango(i) && io.to(port(i)).fire) {
        info.bits(i).pipelineType := 0.U
      }
    }
  }
}
