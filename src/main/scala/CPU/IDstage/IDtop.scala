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

    val ds_allowin = Output(Bool())

    //写回
    val rf_bus = Input(new RegFileBus)

    //分支跳转
    val br_bus = Output(new BranchBus)
}

class IDtop extends Module with Parameters with InstType {
    val io = IO(new IDtop_IO)

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

    io.ds_allowin := io.hand_shake_bf.ready_in
    // val bus = ConnetGetBus(io.hand_shake_bf, io.hand_shake_af)

    //译码获取信息
    val inst = bus.inst
    // val inst = RegInit(0.U(INST_WIDTH.W))
    // when (io.hand_shake_bf.valid_out && io.hand_shake_bf.ready_in) {
    //     inst := bus.inst
    // }
    val List(inst_type, func_type, op_type, is_wf) = 
        ListLookup(inst, List("b0000000".U, "b1".U, "b111111".U, "b1".U), LA64_ALUInst.table)

    val imm = MateDefault(inst_type, 0.U, List(
        Inst2RI8 -> inst(17, 10),
        Inst2RI12 -> SignedExtend(inst(21, 10), 32),
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
    val rkd_value = reg.io.rdata2

    //分支跳转
    val br_bus = WireInit(0.U.asTypeOf(new BranchBus))
    br_bus.valid := MuxCase(false.B, Seq(
            (inst === LA64_ALUInst.JIRL) -> (true.B),
            (inst === LA64_ALUInst.B) -> (true.B),
            (inst === LA64_ALUInst.BL) -> (true.B),
            (inst === LA64_ALUInst.BEQ && rj_value === rkd_value) -> (true.B),
            (inst === LA64_ALUInst.BNE && rj_value =/= rkd_value) -> (true.B)
        )) && valid
    // br_bus.valid := false.B
    br_bus.pc_target := MateDefault(OffWhich(inst_type), 0.U, List(
        OffType.off_rj_or_direct -> (MuxCase(0.U, Seq(
            (inst === LA64_ALUInst.JIRL) -> (rj_value + SignedExtend(Cat(imm, Fill(2, 0.U)), DATA_WIDTH)),
            (inst === LA64_ALUInst.BEQ && rj_value === rkd_value) -> (bus.pc + imm),
            (inst === LA64_ALUInst.BNE && rj_value =/= rkd_value) -> (bus.pc + imm)
        ))),
        OffType.off_pc -> (bus.pc + SignedExtend(Cat(imm, Fill(2, 0.U)), DATA_WIDTH))
    ))
    
    io.br_bus := br_bus

    //传递信息
    val rf_bus = WireInit(0.U.asTypeOf(new RegFileBus))
    rf_bus.valid := is_wf
    rf_bus.waddr := Mux(inst === LA64_ALUInst.BL, 1.U, rd)
    
    val to_next_bus = bus
    to_next_bus.func_type := func_type
    to_next_bus.op_type := op_type
    to_next_bus.src1 := MuxCase(rj_value, Seq(
            (inst === LA64_ALUInst.JIRL) -> (bus.pc),
            (inst === LA64_ALUInst.BL) -> (bus.pc)
        ))
    to_next_bus.src2 := MuxCase(rkd_value, Seq(
            (inst === LA64_ALUInst.JIRL) -> (4.U),
            (inst === LA64_ALUInst.BL) -> (4.U),
            (inst === LA64_ALUInst.SLLI_W
          || inst === LA64_ALUInst.SRLI_W
          || inst === LA64_ALUInst.SRAI_W
          || inst === LA64_ALUInst.ADDI_W
          || inst === LA64_ALUInst.LD_W
          || inst === LA64_ALUInst.ST_W
          || inst === LA64_ALUInst.LU12I_W) -> imm
        ))
    to_next_bus.imm := imm
    to_next_bus.rf_bus := rf_bus
    io.hand_shake_af.bus_out := to_next_bus
}