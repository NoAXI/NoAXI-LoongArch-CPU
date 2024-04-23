package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class ID_IO extends Bundle with Parameters {
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)

  // ** from writeback
  val ws_to_rf_bus = Input(UInt(38.W))

  // ** to if
  val br_bus = Output(new br_bus)
}

class ID extends Module with Parameters with InstType {
  val io = IO(new ID_IO)

  // 握手
  val info = ConnectGetBus(io.from, io.to)

  // 译码获取信息
  val inst = info.inst
  val List(inst_type, func_type, op_type, is_wf) =
    ListLookup(inst, List("b0000000".U, "b1".U, "b111111".U, "b1".U), LA64_ALUInst.table)

  val imm = MateDefault(
    inst_type,
    0.U,
    List(
      Inst2RI8  -> inst(17, 10),
      Inst2RI12 -> SignedExtend(inst(21, 10), 32),
      Inst2RI14 -> inst(23, 10),
      Inst2RI16 -> SignedExtend(Cat(inst(25, 10), Fill(2, 0.U)), 32),
      Inst2RI20 -> SignedExtend(Cat(inst(24, 5), Fill(12, 0.U)), 32),
      Inst2RI26 -> SignedExtend(Cat(inst(9, 0), inst(25, 10)), 32),
      Inst2RUI5 -> inst(14, 10),
      Inst2RUI6 -> inst(15, 10),
      Inst1RI21 -> inst(31, 10),
      // Inst1RCSR -> inst(31, 20),
    ),
  )

  val rj = inst(9, 5)
  val rk = inst(14, 10)
  val rd = inst(4, 0)

  // 写回
  val reg = Module(new REG)
  reg.io.we     := io.ws_to_rf_bus(37)
  reg.io.waddr  := io.ws_to_rf_bus(36, 32)
  reg.io.wdata  := io.ws_to_rf_bus(31, 0)
  reg.io.raddr1 := rj
  reg.io.raddr2 := Mux(
    (inst === LA64_ALUInst.BEQ
      || inst === LA64_ALUInst.BNE
      || inst === LA64_ALUInst.ST_W),
    rd,
    rk,
  )
  val rj_value  = reg.io.rdata1
  val rkd_value = reg.io.rdata2

  // 分支跳转
  io.br_bus.br_taken := MuxCase(
    false.B,
    Seq(
      (inst === LA64_ALUInst.JIRL)                          -> (true.B),
      (inst === LA64_ALUInst.B)                             -> (true.B),
      (inst === LA64_ALUInst.BL)                            -> (true.B),
      (inst === LA64_ALUInst.BEQ && rj_value === rkd_value) -> (true.B),
      (inst === LA64_ALUInst.BNE && rj_value =/= rkd_value) -> (true.B),
    ),
  ) && io.to.valid
  io.br_bus.br_target := MateDefault(
    OffWhich(inst_type),
    0.U,
    List(
      OffType.off_rj_or_direct -> (MuxCase(
        0.U,
        Seq(
          (inst === LA64_ALUInst.JIRL) -> (rj_value + SignedExtend(Cat(imm, Fill(2, 0.U)), 32)),
          (inst === LA64_ALUInst.BEQ && rj_value === rkd_value) -> (info.pc + imm),
          (inst === LA64_ALUInst.BNE && rj_value =/= rkd_value) -> (info.pc + imm),
        ),
      )),
      OffType.off_pc -> (info.pc + SignedExtend(Cat(imm, Fill(2, 0.U)), 32)),
    ),
  )

  // 传递信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.func_type := func_type
  to_info.op_type   := op_type
  to_info.src1 := MuxCase(
    rj_value,
    Seq(
      (inst === LA64_ALUInst.JIRL) -> (info.pc),
      (inst === LA64_ALUInst.BL)   -> (info.pc),
    ),
  )
  to_info.src2 := MuxCase(
    rkd_value,
    Seq(
      (inst === LA64_ALUInst.JIRL) -> (4.U),
      (inst === LA64_ALUInst.BL)   -> (4.U),
      (inst === LA64_ALUInst.SLLI_W
        || inst === LA64_ALUInst.SRLI_W
        || inst === LA64_ALUInst.SRAI_W
        || inst === LA64_ALUInst.ADDI_W
        || inst === LA64_ALUInst.LD_W
        || inst === LA64_ALUInst.ST_W
        || inst === LA64_ALUInst.LU12I_W) -> imm,
    ),
  )

  // 传递信息
  val load_op = false.B // 它没用上
  to_info.func_type := func_type
  to_info.op_type   := op_type
  to_info.is_wf     := is_wf
  to_info.dest      := Mux(inst === LA64_ALUInst.BL, 1.U, rd)
  to_info.rkd_value := rkd_value
  io.to.bits        := to_info
}
