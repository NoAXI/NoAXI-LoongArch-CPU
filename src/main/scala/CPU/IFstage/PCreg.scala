package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class PCreg_IO extends Bundle with Parameters{
    val bf_ready_in = Input(Bool())
    val br_bus = Flipped(new BranchBus)
    val pc = Output(UInt(ADDR_WIDTH.W))
    val next_pc = Output(UInt(ADDR_WIDTH.W))
}

class PCreg extends Module with Parameters {
    val io = IO(new PCreg_IO)

    val pc_reg = RegInit(START_ADDR.U(ADDR_WIDTH.W))
    val next_pc = Mux(io.br_bus.valid, io.br_bus.pc_target, pc_reg + 4.U)
    when(io.bf_ready_in) {
        pc_reg := next_pc
    }
    
    io.pc := pc_reg
    io.next_pc := next_pc
}


