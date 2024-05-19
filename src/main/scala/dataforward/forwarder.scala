package controller

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._

class ForwarderIO extends Bundle {
  val load_complete = Input(Bool())
  val dataIn        = Input(Vec(3, new ForwardData))
  val tag           = Output(Bool())
  val forward_query = Input(new ForwardQuery)
  val forward_ans   = Output(new ForwardAns)
}

class Forwarder extends Module {
  val io = IO(new ForwarderIO)

  io.forward_ans.notld := WireDefault(false.B)
  io.tag               := WireDefault(false.B)

  val hazard_pc = RegInit(0.U(ADDR_WIDTH.W))

  for (i <- 0 until 3) {
    val addr = io.forward_query.addr(i)
    io.forward_ans.data(i) := io.forward_query.ini_data(i)
    for (j <- 0 until 3) {
      when(addr === io.dataIn(j).addr && addr =/= 0.U) {
        when(io.dataIn(j).we) {
          io.forward_ans.data(i) := io.dataIn(j).data
        }
        when(io.dataIn(j).isld) {
          io.tag               := true.B
          io.forward_ans.notld := true.B
        }
      }
    }
  }

  when(io.load_complete) {
    io.forward_ans.notld := false.B
  }
}
