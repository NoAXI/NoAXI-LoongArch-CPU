package controller

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._

class ForwarderIO extends Bundle {
  val load_complete = Input(Bool())
  val dataIn        = Input(Vec(3, new ForwardData))
  val tag           = Output(Bool())
  val tag_pc        = Output(UInt(ADDR_WIDTH.W))
  val forward_query = Input(new ForwardQuery)
  val forward_ans   = Output(new ForwardAns)
}

class Forwarder extends Module {
  val io = IO(new ForwarderIO)

  io.forward_ans.notld := WireDefault(false.B)
  io.tag               := WireDefault(false.B)
  io.tag_pc            := WireDefault(0.U)

  val isnot_same_inst = Wire(Vec(3, Bool()))
  for(i <- 0 until 3) {
    isnot_same_inst(i) := io.forward_query.pc =/= io.dataIn(i).pc
  }

  // to do：前递两个够用了，rj rkd，现在这样做很可能会存在无用的等
  for (i <- 0 until 3) {
    val addr = io.forward_query.addr(i)
    io.forward_ans.data(i) := io.forward_query.ini_data(i)
    for (j <- 0 until 3) {
      when(addr === io.dataIn(j).addr && addr =/= 0.U && isnot_same_inst(j)) {
        when(io.dataIn(j).we) {
          io.forward_ans.data(i) := io.dataIn(j).data
        }
        when(io.dataIn(j).isld) {
          io.tag               := true.B
          io.tag_pc            := io.dataIn(j).pc
          io.forward_ans.notld := true.B
        }
      }
    }
  }

  val csr_addr = io.forward_query.csr_addr
  io.forward_ans.csr_data := io.forward_query.csr_ini_data
  for (j <- 0 until 3) {
    when(csr_addr === io.dataIn(j).csr_addr && io.dataIn(j).csr_we && csr_addr =/= 0.U && isnot_same_inst(j)) {
      io.forward_ans.csr_data := io.dataIn(j).csr_data
    }
  }

  when(io.load_complete) {
    io.forward_ans.notld := false.B
  }
}
