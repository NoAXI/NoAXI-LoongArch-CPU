package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class WritebackTopIO extends SingleStageBundle {
  val preg = Flipped(new PRegWriteIO)
  val rob  = Flipped(new RobWriteIO)
}

class WritebackTop extends Module {
  val io = IO(new WritebackTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = io.to.fire && raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  // writeback -> preg
  io.preg.en    := valid && info.iswf
  io.preg.index := info.rdInfo.preg
  io.preg.data  := info.rdInfo.data

  // writeback -> rob
  io.rob.valid := valid
  io.rob.index := info.robId

  io.rob.bits.done := true.B

  io.rob.bits.wen   := info.iswf
  io.rob.bits.areg  := info.rdInfo.areg
  io.rob.bits.preg  := info.rdInfo.preg
  io.rob.bits.opreg := info.opreg
  io.rob.bits.wdata := info.rdInfo.data

  io.rob.bits.debug_pc  := info.pc
  io.rob.bits.exc_type  := info.exc_type
  io.rob.bits.exc_vaddr := info.exc_vaddr
}
