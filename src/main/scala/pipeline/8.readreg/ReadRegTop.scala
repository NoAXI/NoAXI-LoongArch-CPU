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
  val awake      = Output(new AwakeInfo)
}

class ReadRegTop(
    unitType: String,
) extends Module {
  assert(Seq("arith", "muldiv", "memory").contains(unitType))

  val io = IO(new ReadRegTopIO)

  val busy = WireDefault(false.B)
  val raw  = stageConnect(io.from, io.to, busy)
  flushWhen(raw._1, io.flush)

  val info  = raw._1
  val valid = raw._2
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

  val src1 = MuxCase(
    io.forwardReq.rj.out,
    Seq(
      info.src1Ispc   -> info.pc,
      info.src1IsZero -> 0.U,
    ),
  )
  val src2 = MuxCase(
    io.forwardReq.rk.out,
    Seq(
      info.src2IsFour -> 4.U,
      info.src2IsImm  -> info.imm,
    ),
  )

  // forward -> readreg
  res.rjInfo.data := src1
  res.rkInfo.data := src2

  // arith: awake
  if (unitType == "arith") {
    io.awake.valid := valid && info.iswf && io.to.fire
    io.awake.preg  := info.rdInfo.preg
  } else {
    io.awake := DontCare
  }
}
