package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class IWtop_IO extends Bundle with Parameters {
    //握手
    val hand_shake_bf = new HandShakeBf
    val hand_shake_af = new HandShakeAf

    //debug_sign接口
    val debug_wb_pc = Output(UInt(32.W)) 
    val debug_wb_rf_we = Output(UInt(4.W))
    val debug_wb_rf_wnum = Output(UInt(5.W))
    val debug_wb_rf_wdata = Output(UInt(32.W))

    //写回
    val rf_bus = new RegFileBus
}

class IWtop extends Module with Parameters {
    val io = IO(new IWtop_IO)

    //握手
    val bus = ConnetGetBus(io.hand_shake_bf, io.hand_shake_af)
    io.rf_bus := bus.rf_bus
    io.rf_bus.valid := (bus.rf_bus.valid && io.hand_shake_bf.ready_in
                                       && io.hand_shake_bf.valid_out)
    io.rf_bus.wdata := bus.result

    io.hand_shake_af.bus_out := bus

    io.debug_wb_pc := bus.pc
    io.debug_wb_rf_we := Fill(4, io.rf_bus.valid)
    io.debug_wb_rf_wnum := io.rf_bus.waddr
    io.debug_wb_rf_wdata := io.rf_bus.wdata
}