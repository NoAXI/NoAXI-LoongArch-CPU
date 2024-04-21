package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class IFtop_IO extends Bundle with Parameters {
    //握手
    // val hand_shake_bf = new HandShakeBf
    val hand_shake_af = new HandShakeAf()

    val ds_allowin = Input(Bool())

    //分支
    val br_bus = Input(new BranchBus())

    //指令sram接口
    val inst_sram_rdata = Input(UInt(INST_WIDTH.W))
    val inst_sram_en = Output(Bool())
    val inst_sram_we = Output(UInt((INST_WIDTH / 8).W))
    val inst_sram_addr = Output(UInt(ADDR_WIDTH.W)) 
    val inst_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IFtop extends Module with Parameters {
    val io = IO(new IFtop_IO)

    //握手
    val bf_valid_out = !this.reset.asBool
    // val bf_valid_out = true.B
    val valid = RegInit(false.B)
    val ready_go = true.B
    val bf_ready_in = (!valid) || (ready_go && io.hand_shake_af.ready_in)
    io.hand_shake_af.valid_out := valid && ready_go
    when (bf_ready_in) {
        valid := bf_valid_out
    }

    // when (io.hand_shake_bf.valid_out && io.hand_shake_bf.ready_in) {
    //     bus := io.hand_shake_bf.bus_out
    // }
    // val bus = ConnetGetBus(io.hand_shake_bf, io.hand_shake_af)

    // PCreg
    // val pc_reg = Module(new PCreg)
    // pc_reg.io.br_bus <> io.br_bus
    // pc_reg.io.bf_ready_in := bf_ready_in
    val pc_reg = RegInit(START_ADDR.U(ADDR_WIDTH.W))
    // val pc_reg = RegNext(next_pc, START_ADDR.U(ADDR_WIDTH.W))
    val next_pc = Mux(io.br_bus.valid, io.br_bus.pc_target, pc_reg + 4.U)
    when(bf_valid_out && bf_ready_in) {
        pc_reg := next_pc
    }
    // when(io.hand_shake_bf.valid_out && io.hand_shake_bf.ready_in) {
    //     pc_reg := next_pc
    // }

    //传递信息
    val to_next_bus = WireDefault(0.U.asTypeOf(new Bus))
    to_next_bus.pc := pc_reg
    to_next_bus.inst := io.inst_sram_rdata
    io.hand_shake_af.bus_out := to_next_bus

    //与指令内存的接口
    io.inst_sram_en := bf_valid_out && bf_ready_in
    // io.inst_sram_en := (io.hand_shake_bf.valid_out && io.hand_shake_bf.ready_in)
    io.inst_sram_we := 0.U
    // io.inst_sram_addr := pc_reg.io.next_pc
    io.inst_sram_addr := next_pc
    io.inst_sram_wdata := 6.U
}

