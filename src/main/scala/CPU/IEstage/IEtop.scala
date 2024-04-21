package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class IEtop_IO extends Bundle with Parameters {
    //握手
    val hand_shake_bf = new HandShakeBf
    val hand_shake_af = new HandShakeAf

    //数据sram接口
    val data_sram_en = Output(Bool())
    val data_sram_we = Output(UInt((INST_WIDTH / 8).W))
    val data_sram_addr = Output(UInt(ADDR_WIDTH.W))
    val data_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IEtop extends Module with Parameters {
    val io = IO(new IEtop_IO)

    //握手
    val bus = RegInit(0.U.asTypeOf(new Bus))
    val valid = RegInit(false.B)
    val ready_go = true.B
    io.hand_shake_bf.ready_in := !valid || ready_go && io.hand_shake_af.ready_in
    io.hand_shake_af.valid_out := valid && ready_go
    when (io.hand_shake_bf.ready_in) {
        valid := io.hand_shake_bf.valid_out
    }
    when (io.hand_shake_bf.valid_out && io.hand_shake_bf.ready_in) {
        bus := io.hand_shake_bf.bus_out
    }
    // val bus = ConnetGetBus(io.hand_shake_bf, io.hand_shake_af)

    //计算
    val alu = Module(new Alu)
    alu.io.alu_op := Mux(bus.func_type === FuncType.alu, bus.op_type, 0.U)
    alu.io.alu_src1 := bus.src1
    alu.io.alu_src2 := bus.src2

    val to_next_bus = bus
    to_next_bus.result := bus.result + alu.io.alu_result
    io.hand_shake_af.bus_out := to_next_bus

    io.data_sram_en := true.B
    io.data_sram_we := Mux(bus.func_type === FuncType.mem 
                        && bus.op_type === MemOpType.write
                        && io.hand_shake_bf.ready_in
                        && io.hand_shake_bf.valid_out, 4.U, 0.U)
    io.data_sram_addr := alu.io.alu_result
    io.data_sram_wdata := bus.src2
}