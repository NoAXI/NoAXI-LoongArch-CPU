package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class ForwardAtomicRequestIO extends Bundle {
  val preg = Input(UInt(PREG_WIDTH.W))
  val in   = Input(UInt(DATA_WIDTH.W))
  val out  = Output(UInt(DATA_WIDTH.W))
}

class ForwardRequestIO extends Bundle {
  val rj = new ForwardAtomicRequestIO
  val rk = new ForwardAtomicRequestIO
}

class ForwardInfoIO extends Bundle {
  val preg = Input(UInt(PREG_WIDTH.W))
  val data = Input(UInt(DATA_WIDTH.W))
}

class ForwardIO extends Bundle {
  val req  = Vec(BACK_ISSUE_WIDTH, new ForwardRequestIO)
  val info = Vec(BACK_ISSUE_WIDTH, new ForwardInfoIO)
}

class Forward extends Module {
  val io = IO(new ForwardIO)
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    for (regNum <- 0 to 1) {
      val req = if (i == 0) io.req(i).rj else io.req(i).rk
      req.out := req.in
      for (j <- 0 until BACK_ISSUE_WIDTH) {
        when(req.preg === io.info(j).preg) {
          req.out := io.info(j).data
        }
      }
    }
  }
}
