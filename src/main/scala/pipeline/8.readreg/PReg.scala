package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class PRegAtomicReadIO extends Bundle {
  val index = Input(UInt(PREG_WIDTH.W))
  val data  = Output(UInt(DATA_WIDTH.W))
}

class PRegReadIO extends Bundle {
  val rj = new PRegAtomicReadIO
  val rk = new PRegAtomicReadIO
}

class PRegWriteIO extends Bundle {
  val en    = Input(Bool())
  val index = Input(UInt(PREG_WIDTH.W))
  val data  = Input(UInt(DATA_WIDTH.W))
}

class PRegIO extends Bundle {
  val read  = Vec(BACK_ISSUE_WIDTH, new PRegReadIO)
  val write = Vec(BACK_ISSUE_WIDTH, new PRegWriteIO)

  // val debug_rat = Input(Vec(AREG_NUM, UInt(PREG_WIDTH.W)))
  // val debug_gpr = Output(Vec(AREG_NUM, UInt(DATA_WIDTH.W)))
}

class PReg extends Module {
  val io = IO(new PRegIO)

  val preg = RegInit(VecInit(Seq.fill(PREG_NUM)(0.U(DATA_WIDTH.W))))

  // read
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    val rj = io.read(i).rj
    val rk = io.read(i).rk
    rj.data := preg(rj.index)
    rk.data := preg(rk.index)
  }

  // write
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    val rd = io.write(i)
    when(rd.en && rd.index =/= 0.U) {
      preg(rd.index) := rd.data
    }
  }

  // if (Config.debug_on_chiplab) {
  //   for(i <- 0 until AREG_NUM) {
  //     io.debug_gpr(i) := preg(io.debug_rat(i))
  //   }
  // } else {
  //   io.debug_rat := DontCare
  //   io.debug_gpr := DontCare
  // }
}
