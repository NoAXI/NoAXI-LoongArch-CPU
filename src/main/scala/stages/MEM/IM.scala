package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class IM_IO extends Bundle with Parameters {
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)

  // ** from data-sram
  val data_sram_rdata = Input(UInt(INST_WIDTH.W))
}

class IM extends Module with Parameters {
  val io = IO(new IM_IO)

  // 与上一流水级握手，获取上一流水级信息
  val info = ConnectGetBus(io.from, io.to)

  // 取出上级流水级缓存内容
  val ms_res_from_mem = info.func_type === FuncType.mem && info.op_type === MemOpType.read

  // 传递信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info        := info
  to_info.result := Mux(ms_res_from_mem, io.data_sram_rdata, info.result)
  io.to.bits     := to_info
}
