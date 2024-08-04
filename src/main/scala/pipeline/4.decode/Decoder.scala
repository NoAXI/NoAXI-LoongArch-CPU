package pipeline

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.CSRCodes
import const.Parameters._
import func.Functions._

class DecoderIO extends Bundle {
  val inst = Input(UInt(INST_WIDTH.W))
  val pc   = Input(UInt(ADDR_WIDTH.W))

  val func_type = Output(FuncType())
  val op_type   = Output(UInt(5.W))

  val iswf = Output(Bool())
  val rj   = Output(UInt(AREG_WIDTH.W))
  val rk   = Output(UInt(AREG_WIDTH.W))
  val rd   = Output(UInt(AREG_WIDTH.W))
  val imm  = Output(UInt(DATA_WIDTH.W))

  val isReadCsr  = Output(Bool())
  val isWriteCsr = Output(Bool())
  val csrReg     = Output(UInt(CSR_WIDTH.W))

  val exc_type = Output(ECodes())

  val pipelineType = Output(PipelineType())

  val isCALL   = Output(Bool())
  val isReturn = Output(Bool())

  val src1Ispc   = Output(Bool())
  val src1IsZero = Output(Bool())
  val src2IsFour = Output(Bool())
  val src2IsImm  = Output(Bool())
}

class Decoder extends Module {
  val io = IO(new DecoderIO)

  val stable_counter = Module(new StableCounter).io

  io.rj := io.inst(9, 5)
  io.rk := io.inst(14, 10)

  val List(func_type, op_type) = ListLookup(io.inst, List(0.U, 0.U), LA32R.table)
  io.func_type := func_type
  io.op_type   := op_type

  when(
    func_type === FuncType.alu_imm
      || func_type === FuncType.mem
      || func_type === FuncType.bru
      || io.inst === LA32R.CSRXCHG,
  ) {
    io.rk := 0.U
  }

  when(
    func_type === FuncType.cnt
      || func_type === FuncType.exc
      || (func_type === FuncType.csr && op_type =/= CsrOpType.xchg && op_type =/= CsrOpType.cntrd),
  ) {
    io.rj := 0.U
    io.rk := 0.U
  }

  when(func_type === FuncType.bru || func_type === FuncType.mem || func_type === FuncType.csr) {
    io.rk := io.inst(4, 0)
  }

  val imm05   = io.inst(14, 10)
  val imm12   = SignedExtend(io.inst(21, 10), DATA_WIDTH)
  val imm12u  = UnSignedExtend(io.inst(21, 10), DATA_WIDTH)
  val imm14   = io.inst(23, 10)
  val imm16   = SignedExtend(Cat(io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  val imm20   = SignedExtend(Cat(io.inst(24, 5), Fill(12, 0.U)), DATA_WIDTH)
  val imm26   = SignedExtend(Cat(io.inst(9, 0), io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  val use_imm = func_type === FuncType.alu_imm
  val imm = MateDefault(
    func_type,
    0.U,
    Seq(
      FuncType.csr -> imm14,
      FuncType.mem -> imm12,
      FuncType.bru -> Mux(BruOptype.isimm26(op_type), imm26, imm16),
      FuncType.alu_imm -> MuxCase(
        Mux(AluOpType.isimmu(op_type), imm12u, imm12),
        Seq(
          AluOpType.isimm5(op_type)     -> imm05,
          (io.inst === LA32R.LU12I_W)   -> imm20,
          (io.inst === LA32R.PCADDU12I) -> imm20,
        ),
      ),
    ),
  )
  io.imm := imm
  when(func_type === FuncType.mem && (op_type === MemOpType.ll || op_type === MemOpType.sc)) {
    io.imm := SignedExtend(Cat(imm14, 0.U(2.W)), DATA_WIDTH)
  }

  val is_idle        = func_type === FuncType.alu && op_type === AluOpType.idle
  val is_cacop       = func_type === FuncType.mem && op_type === MemOpType.cacop
  val is_invtlb      = func_type === FuncType.tlb && op_type === TlbOpType.inv
  val is_jirl        = func_type === FuncType.bru && op_type === BruOptype.jirl
  val is_bl          = func_type === FuncType.bru && op_type === BruOptype.bl
  val is_jirl_bl     = is_jirl || is_bl
  val is_none        = func_type === FuncType.none
  val is_exc         = func_type === FuncType.exc
  val is_tlb         = func_type === FuncType.tlb
  val is_st          = func_type === FuncType.mem && MemOpType.iswrite(op_type) && op_type =/= MemOpType.sc
  val br_not_jirl_bl = func_type === FuncType.bru && !is_jirl_bl
  io.iswf := !(is_exc || is_st || is_none || br_not_jirl_bl || is_tlb || is_cacop || is_idle)
  io.rd := MuxCase(
    io.inst(4, 0),
    List(
      (io.inst === LA32R.BL)      -> 1.U,
      (io.inst === LA32R.RDCNTID) -> io.rj,
    ),
  )

  io.src1Ispc   := is_jirl_bl || io.inst === LA32R.PCADDU12I
  io.src1IsZero := io.inst === LA32R.LU12I_W
  io.src2IsImm  := use_imm
  io.src2IsFour := is_jirl_bl

  val is_csr  = func_type === FuncType.csr
  val is_wr   = op_type === CsrOpType.wr
  val is_xchg = op_type === CsrOpType.xchg
  io.isReadCsr  := is_csr
  io.isWriteCsr := is_csr && (is_wr || is_xchg)
  io.csrReg     := Mux(io.inst === LA32R.RDCNTID, CSRCodes.TID, imm(13, 0))
  // io.csr_wmask          := Mux(is_xchg, rj_value, ALL_MASK.U)

  io.exc_type := MuxCase(
    ECodes.NONE,
    List(
      (is_none && io.pc =/= 0.U)             -> ECodes.INE, // inst does not exist and is not caused by flush
      (is_exc && op_type === ExcOpType.brk)  -> ECodes.BRK,
      (is_exc && op_type === ExcOpType.sys)  -> ECodes.SYS,
      (is_exc && op_type === ExcOpType.ertn) -> ECodes.ertn,
      (is_invtlb && io.inst(4, 0) > 6.U)     -> ECodes.INE, // IPE or INE ???
    ),
  )

  io.pipelineType := MuxCase(
    PipelineType.memory,
    List(
      (func_type === FuncType.alu || func_type === FuncType.alu_imm || func_type === FuncType.bru || func_type === FuncType.cnt) -> PipelineType.arith,
      (func_type === FuncType.mul || func_type === FuncType.div || FuncType.isPrivilege(func_type))                              -> PipelineType.muldiv,
    ),
  )

  // dbar = nop
  when(io.inst === LA32R.DBAR || io.inst === LA32R.IDLE) {
    io.rd   := 0.U
    io.rj   := 0.U
    io.rk   := 0.U
    io.iswf := false.B
  }

  // CALL:   BL 或链接返回地址到 $r1 的 JIRL 指令
  // Return: JIRL $r0,$r1,0 指令
  io.isCALL   := is_bl || is_jirl && io.rd === 1.U
  io.isReturn := io.inst === RETURN_ADDR.U
}
