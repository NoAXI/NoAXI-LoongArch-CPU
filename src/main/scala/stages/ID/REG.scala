package stages

import chisel3._
import chisel3.util._

import config._

class REG_IO extends Bundle with Parameters {
  val raddr1 = Input(UInt(ADDR_WIDTH_REG.W))
  val raddr2 = Input(UInt(ADDR_WIDTH_REG.W))
  val rdata1 = Output(UInt(DATA_WIDTH.W))
  val rdata2 = Output(UInt(DATA_WIDTH.W))
  val rf_bus = Input(new rf_bus)
}

class REG extends Module with Parameters {
  val io = IO(new REG_IO)

  val rf = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  when(io.rf_bus.we && io.rf_bus.waddr(4, 0) =/= 0.U) {
    rf(io.rf_bus.waddr(4, 0)) := io.rf_bus.wdata
  }

  io.rdata1 := Mux(io.raddr1 === 0.U, 0.U, rf(io.raddr1))
  io.rdata2 := Mux(io.raddr2 === 0.U, 0.U, rf(io.raddr2))
}
