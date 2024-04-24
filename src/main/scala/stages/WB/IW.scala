package stages

import chisel3._
import chisel3.util._

import config._
import controller._
import config.Functions._

class IW_IO extends Bundle with Parameters {
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)

  val ws = Output(new hazardData)

  // ** to debug_sign
  val debug_wb_pc       = Output(UInt(ADDR_WIDTH.W))
  val debug_wb_rf_we    = Output(UInt(4.W))
  val debug_wb_rf_wnum  = Output(UInt(ADDR_WIDTH_REG.W))
  val debug_wb_rf_wdata = Output(UInt(DATA_WIDTH.W))

  // ** to rf
  val rf_bus = Output(new rf_bus)
}

class IW extends Module with Parameters {
  val io = IO(new IW_IO)

  // 与上一流水级握手，获取上一流水级信息
  val info = ConnectGetBus(io.from, io.to)

  // 写寄存器，传给ds
  io.rf_bus.we    := info.is_wf && io.to.valid
  io.rf_bus.waddr := info.dest
  io.rf_bus.wdata := info.result

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info    := info
  io.to.bits := to_info

  io.ws.we   := to_info.is_wf
  io.ws.addr := to_info.dest
  io.ws.data := to_info.result

  io.debug_wb_pc       := info.pc
  io.debug_wb_rf_we    := Fill(4, io.rf_bus.we)
  io.debug_wb_rf_wnum  := info.dest
  io.debug_wb_rf_wdata := info.result
}
