package pipeline.frontend

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._

class PF_icache_IO extends Bundle {
  val vadrr = Output(UInt(ADDR_WIDTH.W))
  val paddr = Input(UInt(ADDR_WIDTH.W))
}
class PF_tlb_IO extends Bundle {
  
}

class PrefetchTopIO extends StageBundle {
  val icache = new PF_icache_IO
  val tlb    = new PF_tlb_IO
}

class PrefetchTop extends Module {
  val io = IO(new PrefetchTopIO)

}
