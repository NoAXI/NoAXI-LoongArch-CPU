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
class BusyRegUpdateInfo extends Bundle {
  val valid = Bool()
  val preg  = UInt(PREG_WIDTH.W)
}
class DispatchTopIO extends DispatchStageBundle {
  val arithSize = Input(Vec(ARITH_ISSUE_NUM, UInt((ARITH_QUEUE_WIDTH + 1).W)))
  val busyInfo  = Vec(ISSUE_WIDTH, Output(new BusyRegUpdateInfo))
}

class DispatchTop extends Module {
  val io = IO(new DispatchTopIO)

  // stage connect (from)
  // stall means insts use same port, can only send one inst
  val stall     = WireDefault(false.B)
  val portReady = WireDefault(true.B)
  val info      = RegInit(0.U.asTypeOf(new DualInfo))
  val validReg  = RegInit(false.B)
  io.from.ready := !validReg || (portReady && !stall)
  when(io.from.ready) {
    validReg := io.from.valid
  }
  when(io.from.fire) {
    info := io.from.bits
  }
  when(io.flush) {
    info := info.getFlushInfo
  }

  // initial set valid = 0
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    val to = io.to(i)
    to.bits  := 0.U.asTypeOf(to.bits)
    to.valid := false.B
  }

  // select issued pipeline
  val bits  = info.bits
  val cango = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(false.B)))
  val port  = WireDefault(VecInit(Seq.fill(ISSUE_WIDTH)(0.U(log2Ceil(BACK_ISSUE_WIDTH).W))))
  when(bits(0).pipelineType === bits(1).pipelineType) {
    when(bits(0).pipelineType === PipelineType.arith) {
      when(io.to(0).ready && io.to(1).ready) {
        for (i <- 0 until ISSUE_WIDTH) {
          cango(i) := true.B
          port(i)  := i.U
        }
      }.otherwise {
        stall := true.B
        when(io.to(0).ready || io.to(1).ready) {
          for (i <- 0 until ISSUE_WIDTH) {
            when(io.to(i).ready) {
              cango(0) := true.B
              port(0)  := i.U
            }
          }
        }
      }
    }.elsewhen(bits(0).pipelineType =/= PipelineType.nop) {
      cango(0) := true.B
      port(0)  := bits(0).pipelineType
      cango(1) := false.B
      port(1)  := 0.U
      stall    := true.B
    }
  }.otherwise {
    for (i <- 0 until ISSUE_WIDTH) {
      when(bits(i).pipelineType === PipelineType.arith) {
        cango(i) := true.B
        port(i)  := Mux(io.arithSize(0) < io.arithSize(1), 0.U, 1.U)
      }.elsewhen(bits(i).pipelineType =/= PipelineType.nop) {
        cango(i) := true.B
        port(i)  := bits(i).pipelineType
      }
    }
  }
  when(!validReg) {
    for (i <- 0 until ISSUE_WIDTH) {
      cango(i) := false.B
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
        io.to(j).bits  := info.bits(i)
      }
    }
  }
  when(issueFailed.reduce(_ || _)) { stall := true.B }
  when(!validReg) { stall := false.B }

  // when issue not succeed, set stall
  for (i <- 0 until ISSUE_WIDTH) {
    when(cango(i) && !io.to(port(i)).fire) {
      portReady := false.B
    }
  }

  // when stall, flush issued inst
  when(!io.from.ready && !io.flush) {
    for (i <- 0 until ISSUE_WIDTH) {
      when(cango(i) && io.to(port(i)).fire) {
        info.bits(i).pipelineType := 0.U
      }
    }
  }

  // update busyreg
  for (i <- 0 until ISSUE_WIDTH) {
    val to = io.to(port(i))
    io.busyInfo(i).valid := validReg &&
      to.fire &&
      to.bits.rdInfo.preg =/= 0.U &&
      to.bits.iswf &&
      to.bits.pipelineType =/= 0.U
    io.busyInfo(i).preg := to.bits.rdInfo.preg
  }

  if (Config.debug_on) {
    dontTouch(stall)
    dontTouch(cango)
    dontTouch(portReady)
  }
}
