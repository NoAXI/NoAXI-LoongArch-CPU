package stages

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._

class GRRegIO extends Bundle {
  val raddr = Input(Vec(3, UInt(REG_WIDTH.W)))
  val rdata = Output(Vec(3, UInt(DATA_WIDTH.W)))
  val rf_bus = Input(new GRWrite)
}

class GRReg extends Module {
  val io = IO(new GRRegIO)

  val reg = RegInit(VecInit(Seq.fill(32)(0.U(DATA_WIDTH.W))))

  when(io.rf_bus.we && io.rf_bus.waddr =/= 0.U) {
    reg(io.rf_bus.waddr) := io.rf_bus.wdata
  }

  for(i <- 0 until 3) {
    io.rdata(i) := Mux(io.raddr(i) === 0.U, 0.U, reg(io.raddr(i)))
  }
}
