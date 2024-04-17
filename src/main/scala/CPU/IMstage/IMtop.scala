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
    val bus = ConnetGetBus(io.hand_shake_bf, io.hand_shake_af)

    //传递信息
    val to_next_bus = bus
    when(bus.func_type === FuncType.mem && bus.op_type === MemOpType.read) {
        to_next_bus.result := io.data_sram_rdata
    }
    io.hand_shake_af.bus_out := to_next_bus
}