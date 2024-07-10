package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class BRUIO extends Bundle {
  val func_type = Input(FuncType())
  val op_type   = Input(BruOptype())
  val rj        = Input(UInt(DATA_WIDTH.W))
  val rd        = Input(UInt(DATA_WIDTH.W))
  val br_en     = Output(Bool())
}

class BRU extends Module {
  val io = IO(new BRUIO)

  val equal         = io.rj === io.rd
  val unsigned_less = io.rj < io.rd
  val signed_less   = io.rj.asSInt < io.rd.asSInt

  io.br_en := MateDefault(
    io.op_type,
    true.B,
    List(
      BruOptype.beq  -> equal,
      BruOptype.bne  -> !equal,
      BruOptype.blt  -> signed_less,
      BruOptype.bge  -> !signed_less,
      BruOptype.bltu -> unsigned_less,
      BruOptype.bgeu -> !unsigned_less,
    ),
  ) && (io.func_type === FuncType.bru)
}
