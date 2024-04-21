package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class IMtop_IO extends Bundle with Parameters {
    //握手
    val hand_shake_bf = new HandShakeBf
    val hand_shake_af = new HandShakeAf

    //数据sram接口
    val data_sram_rdata = Input(UInt(INST_WIDTH.W))
}

class IMtop extends Module with Parameters {
    val io = IO(new IMtop_IO)

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

    //传递信息
    val to_next_bus = bus
    when(bus.func_type === FuncType.mem && bus.op_type === MemOpType.read) {
        to_next_bus.result := io.data_sram_rdata
    }
    io.hand_shake_af.bus_out := to_next_bus
}