package stages

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._

class PCIO extends Bundle {
  val br       = Input(new br)
  val exc_en   = Input(Bool())
  val en       = Input(Bool())
  val pc       = Output(UInt(ADDR_WIDTH.W))
  val pc_add_4 = Output(UInt(ADDR_WIDTH.W))
  val next_pc  = Output(UInt(ADDR_WIDTH.W))
}

class PC extends Module {
  val io = IO(new PCIO)

  val pc       = RegInit(START_ADDR.U(ADDR_WIDTH.W))
  val pc_add_4 = pc + 4.U
  val next_pc  = Mux(io.br.en, io.br.tar, pc_add_4)

  when(io.en) { pc := next_pc }

  io.pc_add_4 := pc_add_4
  io.pc       := pc
  io.next_pc  := next_pc
}
