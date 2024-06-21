package pipeline.mmu

import chisel3._
import chisel3.util._

import const.Parameters._

class iTLBIO extends Bundle {
  val vaddr = Input(UInt(ADDR_WIDTH.W))
  val paddr = Output(UInt(ADDR_WIDTH.W))
}

class iTLB extends Module {
  val io = IO(new iTLBIO)

  io.paddr := io.vaddr
}
