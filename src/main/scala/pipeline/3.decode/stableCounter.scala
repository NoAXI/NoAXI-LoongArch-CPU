package pipeline

import chisel3._
import chisel3.util._

import const.Parameters._

class StableCounter extends Module {
  val io = IO(new Bundle {
    val counter = Output(UInt(64.W))
  })

  val stable_counter = RegInit(0.U(64.W))
  stable_counter := Mux(stable_counter === Fill(2, ALL_MASK.U), 0.U, stable_counter + 1.U)
  io.counter     := stable_counter
}

