package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

// 清空前端流水级里的指令，未实现

class FetchBPUIO extends Bundle {
  val pc       = Output(UInt(ADDR_WIDTH.W))
  val pc_add_4 = Output(UInt(ADDR_WIDTH.W))
  val res      = Input(new br)
  val stall    = Output(Bool())
}

class FetchTopIO extends StageBundle {
  val tlb    = new FetchTLBIO
  val iCache = new FetchICacheIO
  val bpu    = new FetchBPUIO
  val bpuRes = Output(new br)
}

class FetchTop extends Module {
  val io   = IO(new FetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)

  val info         = from._1.bits(0)
  val valid_signal = from._2

  // get predict result, send back to prefetch
  io.bpu.pc       := info.pc
  io.bpu.stall    := !io.to.ready
  io.bpu.pc_add_4 := info.pc_add_4
  io.bpuRes       := io.bpu.res

  // get paddr from tlb, send to I-Cache
  io.iCache.request.bits  := io.tlb.pa
  io.iCache.request.valid := io.from.fire
  io.iCache.answer.ready  := true.B
  io.iCache.cango         := io.to.ready

  busy.info(0) := !io.iCache.answer.fire

  val is_adef = info.pc(1, 0) =/= "b00".U

  io.to.bits := 0.U.asTypeOf(new DualInfo)
  val to_info = WireDefault(0.U.asTypeOf(new SingleInfo))
  to_info.pc := Mux(io.flush, 0.U, info.pc)
  for (i <- 0 until FETCH_DEPTH) {
    to_info.instV(i).inst  := Mux(is_adef, 0.U, io.iCache.answer.bits(i))
    to_info.instV(i).valid := true.B // TODO: read the jump_index from BTB to decide the valid signal
    to_info.fetchExc(i)    := Mux(is_adef, ECodes.ADEF, ECodes.NONE)
    // TODO: excbadvaddr!!
  }
  io.to.bits.bits(0).predict := Mux(io.flush, 0.U.asTypeOf(new br), io.bpu.res)
  io.to.bits.bits(0)         := Mux(io.flush, 0.U.asTypeOf(new SingleInfo), to_info)
}
