package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class Reg_IO extends Bundle with Parameters {
    val raddr1 = Input(UInt(ADDR_WIDTH_REG.W))
    val raddr2 = Input(UInt(ADDR_WIDTH_REG.W))
    val rdata1 = Output(UInt(DATA_WIDTH.W))
    val rdata2 = Output(UInt(DATA_WIDTH.W))

    val rf_bus = Input(new RegFileBus)
}

class Reg extends Module with Parameters {
    val io = IO(new Reg_IO)

    val reg = RegInit(VecInit(Seq.fill(REG_SIZE)(0.U(ADDR_WIDTH.W))))

    when(io.rf_bus.valid && io.rf_bus.waddr =/= 0.U) {
        reg(io.rf_bus.waddr) := io.rf_bus.wdata
    }

    io.rdata1 := Mux(io.raddr1 === 0.U, 0.U, reg(io.raddr1))
    io.rdata2 := Mux(io.raddr2 === 0.U, 0.U, reg(io.raddr2))
}