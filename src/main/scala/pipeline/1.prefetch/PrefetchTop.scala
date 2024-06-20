package pipeline.frontend

import chisel3._
import chisel3.util._

import const._
import bundles._
import Funcs.Functions._
import const.Parameters._

class PF_icache_IO extends Bundle {
  val inst = Input(UInt(DATA_WIDTH.W))
}
class PF_tlb_IO extends Bundle {
  val vaddr = Output(UInt(ADDR_WIDTH.W))
  val hit   = Input(UInt(tlbConst.TLB_ENTRIES.W))
}

class PrefetchTopIO extends StageBundle {
  val icache = new PF_icache_IO
  val tlb    = new PF_tlb_IO
}

class PrefetchTop extends Module {
  val io   = IO(new PrefetchTopIO)
  val busy = WireDefault(false.B)
  StageConnect(io.from, io.to, busy)

  val pc        = RegInit(UInt(ADDR_WIDTH.W))
  val pc_plus_8 = pc + 8.U
  val pc_plus_4 = pc + 4.U
  val pc_next   = WireDefault(pc_plus_8)
  when(pc(0).asBool) {
    pc_next := pc_plus_4
  }

  val bpu = Module(new BPU)

}
