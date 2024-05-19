package stages

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._

class PCIO extends Bundle {
  val br      = Input(new br)
  val flush   = Input(Bool())
  val en      = Input(Bool())
  val pc      = Output(UInt(ADDR_WIDTH.W))
  val next_pc = Output(UInt(ADDR_WIDTH.W))
}

class PC extends Module {
  val io = IO(new PCIO)

  val pc = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val next_pc = MuxCase(
    pc + 4.U,
    Seq(
      //   io.exc_bus.en                      -> io.exc_bus.pc,
      //   (io.br_bus.br_taken && !bf_exc_en) -> io.br_bus.br_target,
      io.br.en -> io.br.tar,
    ),
  )

  when(io.en) { pc := next_pc }

  io.pc      := pc
  io.next_pc := next_pc
}
