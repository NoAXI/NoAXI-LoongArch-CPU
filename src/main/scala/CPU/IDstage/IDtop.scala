package CPU

import chisel3._
import chisel3.util._

import Parameters.Functions._
import Parameters._
import HandShake._
import OtherBus._

class IDtop_IO extends Bundle with Parameters {
    //握手
    val hand_shake_bf = new HandShakeBf
    val hand_shake_af = new HandShakeAf

    //写回
    val rf_bus = Flipped(new RegFileBus)

    //分支跳转
    val br_bus = new BranchBus
}

class IDtop extends Module with Parameters with InstType {
    val io = IO(new IDtop_IO)

    //握手
    val bus = ConnetGetBus(io.hand_shake_bf, io.hand_shake_af)

    //译码获取信息
    val inst = io.hand_shake_bf.bus_out.inst
    val List(inst_type, func_type, op_type, is_wf) = 
        ListLookup(inst, List("b11111".U, "b1".U, "b111111".U, "b1".U), LA64_ALUInst.table)

    val imm = MateDefault(inst_type, 0.U, List(
        Inst2RI8 -> inst(17, 10),
        Inst2RI12 -> inst(21, 10),
        Inst2RI14 -> inst(23, 10),
        Inst2RI16 -> inst(25, 10),
        Inst2RI20 -> inst(26, 5),
        Inst2RI26 -> Cat(inst(9, 0), inst(25, 10)),
        Inst2RUI5 -> inst(14, 10),
        Inst2RUI6 -> inst(15, 10),
        Inst1RI21 -> inst(31, 10),
        // Inst1RCSR -> inst(31, 20),
    ))

    val rj = inst(9, 5)
    val rk = inst(14, 10)
    val rd = inst(4, 0)

    //写回
    val reg = Module(new Reg)
    reg.io.rf_bus <> io.rf_bus
    reg.io.raddr1 := rj
    reg.io.raddr2 := Mux((inst === LA64_ALUInst.BEQ 
                       || inst === LA64_ALUInst.BNE 
                       || inst === LA64_ALUInst.ST_W), rd, rk)
    val rj_value = reg.io.rdata1
    val rk_value = reg.io.rdata2

    //分支跳转
    io.br_bus.valid := IsBranch(inst_type)
    io.br_bus.pc_target := MateDefault(OffWhich(inst_type), 0.U, List(
        OffType.off_rj_or_direct -> (MuxCase(0.U, Seq(
            (inst === LA64_ALUInst.JIRL) -> (rj_value + SignedExtend(Cat(imm, Fill(2, 0.U)), DATA_WIDTH)),
            (inst === LA64_ALUInst.BEQ && rj_value === rk_value) -> (bus.pc + imm),
            (inst === LA64_ALUInst.BNE && rj_value =/= rk_value) -> (bus.pc + imm)
        ))),
        OffType.off_pc -> (bus.pc + SignedExtend(Cat(imm, Fill(2, 0.U)), DATA_WIDTH))
    ))

    //传递信息
    val rf_bus = WireInit(0.U.asTypeOf(new RegFileBus))
    rf_bus.valid := is_wf
    rf_bus.waddr := Mux(inst === LA64_ALUInst.BL, 1.U, rd)
    
    val to_next_bus = bus
    to_next_bus.func_type := func_type
    to_next_bus.op_type := op_type
    to_next_bus.src1 := rj_value
    to_next_bus.src2 := rk_value
    to_next_bus.imm := imm
    to_next_bus.rf_bus := rf_bus
    io.hand_shake_af.bus_out := to_next_bus
}