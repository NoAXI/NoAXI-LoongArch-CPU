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

  val info         = from._1.bits(0)
  val valid_signal = from._2

  // get paddr from tlb, send to I-Cache
  io.iCache.request.bits  := io.tlb.pa
  io.iCache.request.valid := io.from.fire
  io.iCache.request.ready := true.B
  io.iCache.cango         := io.to.ready

  busy := !io.iCache.answer.fire

  val is_adef = info.pc(1, 0) =/= "b00".U

  val to_info = WireDefault(0.U.asTypeOf(new SingleInfo))
  to_info.pc := info.pc
  for (i <- 0 until FETCH_DEPTH) {
    to_info.instV(i).inst  := Mux(is_adef, 0.U, io.iCache.answer.bits(i))
    to_info.instV(i).valid := true.B // TODO: read the jump_index from BTB to decide the valid signal
    to_info.fetchExc(i)    := Mux(is_adef, ECodes.ADEF, ECodes.NONE)
  }
  io.to.bits.bits(0).instV := to_info
}
