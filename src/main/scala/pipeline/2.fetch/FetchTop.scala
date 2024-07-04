package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

// 清空前端流水级里的指令，未实现
// TODO: connect the fetch and IB !!
class FetchTopIO extends StageBundle {
  val tlb    = new FetchTLBIO
  val iCache = new FetchICacheIO
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info         = from._1.bits
  val valid_signal = from._2

  // get paddr from tlb, send to I-Cache
  io.iCache.request.bits  := io.tlb.pa
  io.iCache.request.valid := io.from.fire
  io.iCache.request.ready := true.B
  io.iCache.cango         := io.to.ready

  busy := !io.iCache.answer.fire
  val is_adef = info(0).pc(1, 0) =/= "b00".U

  val to_info = Vec(4, WireDefault(0.U.asTypeOf(new SingleInfo)))
  for (i <- 0 until 4) {
    to_info(i)           := info(0)
    to_info(i).inst      := Mux(is_adef, 0.U, io.iCache.answer.bits(i))
    to_info(i).exc_type  := Mux(is_adef, ECodes.ADEF, ECodes.NONE)
    to_info(i).exc_vaddr := info(0).pc
  }
  io.to.bits := to_info
}
