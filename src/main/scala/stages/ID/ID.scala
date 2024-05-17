package stages

import chisel3._
import chisel3.util._

import isa._
import csr._
import config._
import controller._
import config.Functions._

class ID_IO extends Bundle with Parameters {
  // handshake
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)

  // act with controller
  val ds_reg_info = Output(new dsRegInfo)
  val this_exc    = Output(Bool())
  val has_exc     = Input(Bool())
  val ds_reg_data = Input(new dsRegData)

  // act with csr
  val csr_re     = Output(Bool())
  val csr_raddr  = Output(UInt(14.W))
  val csr_rf_bus = Output(new rf_bus)
  val csr_rdata  = Input(UInt(DATA_WIDTH.W))
  val counter    = Input(UInt(64.W))

  // from writeback: write register
  val rf_bus   = Input(new rf_bus)
  val rcsr_bus = Input(new rf_bus)

  // to fetch: branch
  val br_bus = Output(new br_bus)
}

class ID extends Module with Parameters with InstType {
  val io = IO(new ID_IO)

  val info = ConnectGetBus(io.from, io.to)

  when(io.has_exc) {
    info := WireDefault(0.U.asTypeOf(new info))
  }

  // 译码获取信息
  val inst = info.inst
  val List(inst_type, func_type, op_type, is_wf, src_type1, src_type2) =
    ListLookup(inst, List("b0000000".U, "b111".U, "b111111".U, "b0".U, "b111".U, "b111".U), LA32.table)

  val imm = MateDefault(
    inst_type,
    0.U,
    List(
      Inst2RI8   -> inst(17, 10),
      Inst2RI12  -> Extend(inst(21, 10), DATA_WIDTH, src_type2),
      Inst2RI14  -> inst(23, 10),
      Inst2RI16  -> Extend(Cat(inst(25, 10), Fill(2, 0.U)), DATA_WIDTH, src_type2),
      Inst2RI20  -> Extend(Cat(inst(24, 5), Fill(12, 0.U)), DATA_WIDTH, src_type2),
      Inst2RI26  -> SignedExtend(Cat(inst(9, 0), inst(25, 10), Fill(2, 0.U)), DATA_WIDTH),
      Inst2RUI5  -> inst(14, 10),
      Inst2RUI6  -> inst(15, 10),
      Inst1RI21  -> inst(31, 10),
      Inst0Rcode -> inst(14, 0),
      InstCSR14  -> inst(23, 10),
    ),
  )

  val rj = inst(9, 5)
  val rk = inst(14, 10)
  val rd = inst(4, 0)

  // 写回
  val reg = Module(new REG)
  reg.io.rf_bus := io.rf_bus
  io.csr_rf_bus := io.rcsr_bus

  // 读取
  reg.io.raddr1 := rj
  reg.io.raddr2 := MateDefault(
    src_type2,
    rk,
    List(
      SrcType.rd     -> rd,
      SrcType.rd_imm -> rd,
    ),
  )
  io.csr_raddr := Mux(inst === LA32.RDCNTID, CSRCodes.TID, imm(13, 0))
  io.csr_re    := func_type === FuncType.csr

  // 前递处理
  io.ds_reg_info.addr         := Seq(reg.io.raddr1, reg.io.raddr2)
  io.ds_reg_info.csr_addr     := imm(13, 0)
  io.ds_reg_info.ini_data     := Seq(reg.io.rdata1, reg.io.rdata2)
  io.ds_reg_info.csr_ini_data := io.csr_rdata

  val rj_value  = io.ds_reg_data.data(0)
  val rkd_value = io.ds_reg_data.data(1)

  val es_mem_re = func_type === FuncType.mem && MemOpType.isread(op_type)
  val es_mem_we = func_type === FuncType.mem && !es_mem_re
  val result    = rj_value + imm

  val exception = Mux(
    io.to.valid && es_mem_we && (!io.has_exc) || es_mem_re,
    MuxCase(
      ECodes.NONE,
      List(
        (op_type === MemOpType.writeh && (result(0) =/= "b0".U))     -> ECodes.ALE,
        (op_type === MemOpType.writew && (result(1, 0) =/= "b00".U)) -> ECodes.ALE,
        (op_type === MemOpType.readh && (result(0) =/= "b0".U))      -> ECodes.ALE,
        (op_type === MemOpType.readhu && (result(0) =/= "b0".U))     -> ECodes.ALE,
        (op_type === MemOpType.readw && (result(1, 0) =/= "b00".U))  -> ECodes.ALE,
        // (result(31) === 1.U) -> ECodes.ADEM,
      ),
    ),
    MuxCase(
      ECodes.NONE,
      List(
        (op_type === "b111111".U && info.pc =/= 0.U)               -> ECodes.INE, // 意思就是说没这个指令，并且不是flush导致的
        (func_type === FuncType.exc && op_type === ExcOpType.brk)  -> ECodes.BRK,
        (func_type === FuncType.exc && op_type === ExcOpType.sys)  -> ECodes.SYS,
        (func_type === FuncType.exc && op_type === ExcOpType.ertn) -> ECodes.ertn,
      ),
    ),
  )

  io.this_exc    := Mux(info.this_exc, info.this_exc, exception =/= ECodes.NONE)

  // 分支跳转
  io.br_bus.br_taken := MateDefault(
    op_type,
    true.B,
    List(
      BruOptype.beq  -> (rj_value === rkd_value),
      BruOptype.bne  -> (rj_value =/= rkd_value),
      BruOptype.blt  -> (rj_value.asSInt < rkd_value.asSInt),
      BruOptype.bge  -> (rj_value.asSInt > rkd_value.asSInt),
      BruOptype.bltu -> (rj_value < rkd_value),
      BruOptype.bgeu -> (rj_value > rkd_value),
    ),
  ) && io.to.valid && (func_type === FuncType.bru)
  io.br_bus.br_target := Mux(inst === LA32.JIRL, rj_value + imm, info.pc + imm)

  // 传递信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.func_type := func_type
  to_info.op_type   := op_type
  to_info.src1 := MateDefault(
    src_type1,
    rj_value,
    List(
      SrcType.pc -> (info.pc),
    ),
  )
  to_info.src2 := MateDefault(
    src_type2,
    rkd_value,
    List(
      SrcType.is4    -> (4.U),
      SrcType.imm    -> imm,
      SrcType.immu   -> imm,
      SrcType.rd_imm -> imm,
      SrcType.rk     -> rkd_value,
      SrcType.rd     -> rkd_value,
    ),
  )

  to_info.func_type := func_type
  to_info.op_type   := op_type
  to_info.is_wf     := is_wf
  to_info.dest := MuxCase(
    rd,
    List(
      (inst === LA32.BL)      -> 1.U,
      (inst === LA32.RDCNTID) -> rj,
    ),
  )
  to_info.rkd_value := rkd_value
  to_info.csr_addr  := imm(13, 0)
  to_info.csr_val := MuxCase(
    io.ds_reg_data.csr_val,
    List(
      (func_type === FuncType.csr && op_type === CsrOpType.cnth) -> io.counter(63, 32),
      (func_type === FuncType.csr && op_type === CsrOpType.cntl) -> io.counter(31, 0),
    ),
  )
  to_info.csr_mask   := rj_value
  to_info.csr_we     := func_type === FuncType.csr && (op_type === CsrOpType.wr || op_type === CsrOpType.xchg)
  to_info.ecode      := imm(14, 0)
  to_info.this_exc   := io.this_exc
  to_info.exc_type   := Mux(info.this_exc, info.exc_type, exception)
  to_info.wrong_addr := Mux(info.this_exc, info.wrong_addr, result)
  io.to.bits         := to_info
}
