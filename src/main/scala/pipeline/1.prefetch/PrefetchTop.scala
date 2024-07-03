package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class PrefetchTopIO extends StageBundle {}

class PrefetchTop extends Module {
  val io   = IO(new PrefetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  stageConnect(io.from, io.to, busy)

  val bpu = Module(new BPU)

  val pc        = RegInit(UInt(ADDR_WIDTH.W))
  val pc_plus_8 = pc + 8.U
  val pc_plus_4 = pc + 4.U
  val pc_next   = WireDefault(pc_plus_8)
  when(pc(0).asBool) {
    pc_next := pc_plus_4
  }
}
