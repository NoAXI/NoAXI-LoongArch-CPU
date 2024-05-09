package stages

import chisel3._
import chisel3.util._

import isa._
import csr._
import config._
import config.Functions._

class IF_IO extends Bundle with Parameters {
  // handshake
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)

  // act with controller
  val flush_en    = Input(Bool())
  val flush_apply = Output(UInt(5.W))
  val this_exc    = Output(Bool())
  val has_exc     = Input(Bool())

  // from csr: exception
  val exc_bus = Input(new exc_bus)

  // from decoder: branch
  val br_bus = Input(new br_bus)

  // act with inst_sram
  val inst_sram_en    = Output(Bool())
  val inst_sram_we    = Output(UInt(INST_WIDTH_B.W))
  val inst_sram_addr  = Output(UInt(ADDR_WIDTH.W))
  val inst_sram_wdata = Output(UInt(INST_WIDTH.W))
  val inst_sram_rdata = Input(UInt(INST_WIDTH.W))
}

class IF extends Module with Parameters {
  val io = IO(new IF_IO)

  val info = ConnectGetBus(io.from, io.to)

  val bf_exc_en = ShiftRegister(io.exc_bus.en, 1)
  val pc        = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val next_pc = MuxCase(
    pc + 4.U,
    Seq(
      io.exc_bus.en                      -> io.exc_bus.pc,
      (io.br_bus.br_taken && !bf_exc_en) -> io.br_bus.br_target,
    ),
  )

  // 若是分支但前一拍是异常入口，则无需跳转且正常流水
  when(io.br_bus.br_taken && !bf_exc_en) {
    io.to.valid := false.B
  }

  when((io.from.valid && io.from.ready && !io.this_exc) || io.exc_bus.en) {
    pc := next_pc
  }

  io.flush_apply := 0.U
  io.this_exc    := pc(1, 0) =/= "b00".U

  // 传递流水信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info.exc_type := ECodes.NONE
  to_info.pc       := pc
  to_info.inst     := io.inst_sram_rdata
  to_info.this_exc := io.this_exc
  when(io.this_exc) {
    to_info.exc_type   := ECodes.ADEF
    to_info.wrong_addr := pc
  }
  io.to.bits := to_info

  // 与指令内存的接口
  io.inst_sram_en    := (io.from.valid && io.from.ready) || io.exc_bus.en
  io.inst_sram_we    := 0.U
  io.inst_sram_addr  := next_pc
  io.inst_sram_wdata := 0.U
}
