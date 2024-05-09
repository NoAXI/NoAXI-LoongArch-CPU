package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class IF_IO extends Bundle with Parameters {
  val from        = Flipped(DecoupledIO(new info))
  val to          = DecoupledIO(new info)
  val flush_en    = Input(Bool())
  val flush_apply = Output(UInt(5.W))

  val this_exc = Output(Bool())
  val has_exc  = Input(Bool())

  // ** from csr
  val exc_bus = Input(new exc_bus)

  // ** from ds
  val br_bus = Input(new br_bus)

  // ** from and to sram
  val inst_sram_en    = Output(Bool())
  val inst_sram_we    = Output(UInt(INST_WIDTH_B.W))
  val inst_sram_addr  = Output(UInt(ADDR_WIDTH.W))
  val inst_sram_wdata = Output(UInt(INST_WIDTH.W))
  val inst_sram_rdata = Input(UInt(INST_WIDTH.W))
}

class IF extends Module with Parameters {
  val io = IO(new IF_IO)

  // 与下一流水级握手
  val info = ConnectGetBus(io.from, io.to)
  io.flush_apply := 0.U

  // 分支 or 正常 PCReg的跳转
  val bf_exc_en = ShiftRegister(io.exc_bus.en, 1)
  val fs_pc   = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val next_pc = Mux(io.exc_bus.en, io.exc_bus.pc, Mux(io.br_bus.br_taken && !bf_exc_en, io.br_bus.br_target, fs_pc + 4.U))
  when(io.br_bus.br_taken && !bf_exc_en) { // 若是分支但前一拍是异常入口，测无需跳转且正常流水
    io.to.valid := false.B
  }
  when((io.from.valid && io.from.ready && !io.this_exc) || io.exc_bus.en) {
    fs_pc := next_pc
  }

  io.this_exc := fs_pc(1, 0) =/= "b00".U

  // 传递流水信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info.exc_type := ECodes.NONE
  to_info.pc       := fs_pc
  to_info.inst     := io.inst_sram_rdata
  to_info.this_exc := io.this_exc
  when(io.this_exc) {
    to_info.exc_type := ECodes.ADEF
    to_info.wrong_addr := fs_pc
  }
  io.to.bits := to_info

  // 与指令内存的接口
  io.inst_sram_en    := (io.from.valid && io.from.ready) || io.exc_bus.en
  io.inst_sram_we    := 0.U
  io.inst_sram_addr  := next_pc
  io.inst_sram_wdata := 0.U
}
