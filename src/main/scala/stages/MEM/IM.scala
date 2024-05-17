package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import controller._
import config.Functions._

class IM_IO extends Bundle with Parameters {
  val from        = Flipped(DecoupledIO(new info))
  val to          = DecoupledIO(new info)

  val this_exc = Output(Bool())
  val has_exc  = Input(Bool())

  val ms     = Output(new hazardData)
  val csr_ms = Output(new hazardData)

  // ** from data-sram
  // val data_sram_rdata = Input(UInt(INST_WIDTH.W))
}

class IM extends Module with Parameters {
  val io = IO(new IM_IO)

  // 与上一流水级握手，获取上一流水级信息
  val info = ConnectGetBus(io.from, io.to)
  when(io.has_exc) {
    info := WireDefault(0.U.asTypeOf(new info))
  }
  io.this_exc    := info.this_exc

  // 传递信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info    := info
  io.to.bits := to_info

  // 前递
  io.ms.we       := to_info.is_wf
  io.ms.addr     := to_info.dest
  io.ms.data     := to_info.result
  io.csr_ms.we   := to_info.csr_we
  io.csr_ms.addr := to_info.csr_addr
  io.csr_ms.data := to_info.rkd_value
}
