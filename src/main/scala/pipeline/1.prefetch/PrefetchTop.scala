package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

// Predict Failed：需要清空前端流水级里的指令，未实现

class PreFetchBPUIO extends Bundle {
  val pc    = Output(UInt(ADDR_WIDTH.W))
  val train = Output(new BpuTrain)
}

class PrefetchTopIO extends StageBundle {
  val iCache   = new PreFetchICacheIO
  val tlb      = new PreFetchTLBIO
  val bpu      = new PreFetchBPUIO
  val bpuRes   = Input(new br)       // 预测结果/异常跳转
  val bpuTrain = Input(new BpuTrain) // 跳转结果
}

class PrefetchTop extends Module {
  val io   = IO(new PrefetchTopIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  stageConnect(io.from, io.to, busy)

  val pc      = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val next_pc = WireDefault(0.U(ADDR_WIDTH.W))

  // send to tlb
  io.tlb.va := pc

  // VI to iCache
  io.iCache.request.valid     := io.from.fire
  io.iCache.request.bits.addr := pc

  // just send pc to bpu
  io.bpu.pc    := pc
  io.bpu.train := io.bpuTrain

  next_pc := Mux(!io.bpuTrain.succeed, io.bpuTrain.target, Mux(io.bpuRes.en, io.bpuRes.tar, nextLine(pc)))
  pc      := next_pc

  // just send the fetch group's first pc
  io.to.bits                  := 0.U.asTypeOf(new DualInfo)
  io.to.bits.bits(0).pc       := pc
  io.to.bits.bits(0).pc_add_4 := pc + 4.U
}
