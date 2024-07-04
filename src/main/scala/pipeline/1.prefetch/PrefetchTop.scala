package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

// Predict Failed：需要清空前端流水级里的指令，未实现

class PrefetchTopIO extends StageBundle {
  val iCache   = new PreFetchICacheIO
  val tlb      = new PreFetchTLBIO
  val bpuTrain = Input(new BpuTrain) // 跳转结果
}

class PrefetchTop extends Module {
  val io   = IO(new PrefetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  stageConnect(io.from, io.to, busy)

  val pc      = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val next_pc = WireDefault(0.U(ADDR_WIDTH.W))
  val bpu     = Module(new BPU).io

  io.tlb.va              := pc
  io.iCache.request.bits := pc

  bpu.pc    := pc
  bpu.train := io.bpuTrain

  next_pc := Mux(!io.bpuTrain.succeed, io.bpuTrain.target, Mux(bpu.res.en, bpu.res.addr, nextLine(pc)))
  pc      := next_pc

  val toInfo = WireDefault(0.U.asTypeOf(new SingleInfo))
  toInfo.pc          := pc
  io.to.bits.bits(0) := toInfo
}
