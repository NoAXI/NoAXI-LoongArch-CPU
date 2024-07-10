package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class MemBufferIO extends Bundle {
  val pa   = Output(UInt(ADDR_WIDTH.W))
  val hit  = Input(Bool())
  val data = Input(UInt(DATA_WIDTH.W))
}

class Memory2TopIO extends SingleStageBundle {
  val dCache   = new Mem2DCacheIO
  val forward  = Flipped(new ForwardInfoIO)
  val stBuffer = new MemBufferIO
  val wbBuffer = new MemBufferIO
}

class Memory2Top extends Module {
  val io = IO(new Memory2TopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
  val res   = WireDefault(info)
  io.to.bits := res

  res.rdInfo.data := MuxCase(
    io.dCache.data,
    Seq(
      io.stBuffer.hit -> io.stBuffer.data,
      io.wbBuffer.hit -> io.wbBuffer.data,
    ),
  )

  doForward(io.forward, res, valid)
}
