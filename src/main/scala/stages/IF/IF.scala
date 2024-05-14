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

  // act with icache
  val fetch   = Flipped(DecoupledIO(UInt(INST_WIDTH.W)))
  val request = Output(Bool())
  val finish  = Output(Bool())
  val addr    = Output(UInt(ADDR_WIDTH.W))
}

class IF extends Module with Parameters {
  val io = IO(new IF_IO)

  val info = ConnectGetBus(io.from, io.to)

  val bf_exc_en = ShiftRegister(io.exc_bus.en, 1)
  val pc        = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val br_taken  = RegInit(false.B)

  when(io.br_bus.br_taken) {
    br_taken := true.B
  }.elsewhen(io.to.valid) {
    br_taken := false.B
  } // 设计的不太好，会导致br_taken持续过长

  val br_en = br_taken || io.br_bus.br_taken

  val next_pc = MuxCase(
    pc + 4.U,
    Seq(
      io.exc_bus.en            -> io.exc_bus.pc,
      (br_en && !bf_exc_en) -> io.br_bus.br_target,
    ),
  )

  // 若是分支但前一拍是异常入口，则无需跳转且正常流水
  when(io.br_bus.br_taken && !bf_exc_en || !ShiftRegister(io.fetch.valid, 1)) {
    io.to.valid := false.B
  }

  when(((io.from.valid && io.from.ready && !io.this_exc) || io.exc_bus.en) && io.fetch.valid) {
    pc := next_pc
  }

  io.flush_apply := 0.U
  io.this_exc    := pc(1, 0) =/= "b00".U

  // 传递流水信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info.exc_type := ECodes.NONE
  to_info.pc       := pc
  to_info.inst     := io.fetch.bits // to do: io.fetch.valid有什么用
  to_info.this_exc := io.this_exc
  when(io.this_exc) {
    to_info.exc_type   := ECodes.ADEF
    to_info.wrong_addr := pc
  }
  io.to.bits := to_info

  // 与指令内存的接口
  io.fetch.ready := true.B
  io.request     := (io.from.fire || io.exc_bus.en) && (!io.fetch.valid && !ShiftRegister(io.fetch.valid, 1)) // vivado专属傻呗版本
  // io.finish      := io.to.fire // ?
  io.finish := true.B
  // io.finish := next_pc =/= ShiftRegister(next_pc, 1)
  io.addr   := next_pc 
}
