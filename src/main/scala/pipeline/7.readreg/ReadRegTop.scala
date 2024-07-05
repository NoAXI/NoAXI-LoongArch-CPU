package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class ReadRegTopIO extends SingleStageBundle {
  val forwardReq = Flipped(new ForwardRequestIO)
  val pregRead   = Flipped(new PRegReadIO)
}

class ReadRegTop extends Module {
  val io = IO(new ReadRegTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = io.to.fire && raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  // readreg -> preg
  io.pregRead.rj.index := info.rjInfo.preg
  io.pregRead.rk.index := info.rkInfo.preg

  // readreg -> forward
  io.forwardReq.rj.preg := info.rjInfo.preg
  io.forwardReq.rj.in   := io.pregRead.rj.data
  io.forwardReq.rk.preg := info.rkInfo.preg
  io.forwardReq.rk.in   := io.pregRead.rk.data

  // forward -> readreg
  res.rjInfo.data := io.forwardReq.rj.out
  res.rkInfo.data := io.forwardReq.rk.out
}
