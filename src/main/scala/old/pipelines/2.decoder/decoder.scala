package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.CSRCodes
import const.Parameters._
import Funcs.Functions._

class DecoderIO extends Bundle {
  val inst = Input(UInt(INST_WIDTH.W))
  val pc   = Input(UInt(ADDR_WIDTH.W))
  val data = Input(Vec(3, UInt(DATA_WIDTH.W)))

  val func_type = Output(FuncType())
  val op_type   = Output(UInt(5.W))
  val isload    = Output(Bool())

  val imm  = Output(UInt(DATA_WIDTH.W))
  val src1 = Output(UInt(DATA_WIDTH.W))
  val src2 = Output(UInt(DATA_WIDTH.W))

  val iswf  = Output(Bool())
  val wfreg = Output(UInt(REG_WIDTH.W))

  val csr_reg_read = new csrRegRead
  val csr_iswf     = Output(Bool())
  val csr_wmask    = Output(UInt(DATA_WIDTH.W))
  val csr_wfreg    = Output(UInt(CSR_WIDTH.W))
  val csr_value    = Output(UInt(DATA_WIDTH.W))

  val exc_type = Output(ECodes())
}

class Decoder extends Module {
  val io = IO(new DecoderIO)

  val stable_counter = Module(new StableCounter).io

  val rj = io.inst(9, 5)
  val rk = io.inst(14, 10)
  val rd = io.inst(4, 0)

  val rj_value = io.data(0)
  val rk_value = io.data(1)
  val rd_value = io.data(2)

  val List(func_type, op_type) = ListLookup(io.inst, List(0.U, 0.U), LA32R.table)
  io.func_type := func_type
  io.op_type   := op_type
  io.isload    := func_type === FuncType.mem && MemOpType.isread(op_type)

  val imm05   = io.inst(14, 10)
  val imm12   = SignedExtend(io.inst(21, 10), DATA_WIDTH)
  val imm12u  = UnSignedExtend(io.inst(21, 10), DATA_WIDTH)
  val imm14   = io.inst(23, 10)
  val imm16   = SignedExtend(Cat(io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  val imm20   = SignedExtend(Cat(io.inst(24, 5), Fill(12, 0.U)), DATA_WIDTH)
  val imm26   = SignedExtend(Cat(io.inst(9, 0), io.inst(25, 10), Fill(2, 0.U)), DATA_WIDTH)
  val use_imm = func_type === FuncType.mem || func_type === FuncType.bru || func_type === FuncType.alu_imm
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

  val is_jirl_bl = func_type === FuncType.bru && (op_type === BruOptype.jirl || op_type === BruOptype.bl)

  io.src1 := MuxCase(
    rj_value,
    Seq(
      is_jirl_bl                    -> io.pc,
      (io.inst === LA32R.LU12I_W)   -> 0.U,
      (io.inst === LA32R.PCADDU12I) -> io.pc,
    ),
  )
  io.src2 := MuxCase(
    rk_value,
    Seq(
      is_jirl_bl -> 4.U,
      use_imm    -> imm,
    ),
  )

  val is_none        = func_type === FuncType.none
  val is_exc         = func_type === FuncType.exc
  val is_st          = func_type === FuncType.mem && !MemOpType.isread(op_type)
  val br_not_jirl_bl = func_type === FuncType.bru && !is_jirl_bl
  io.iswf := !(is_exc || is_st || is_none || br_not_jirl_bl)
  io.wfreg := MuxCase(
    rd,
    List(
      (io.inst === LA32R.BL)      -> 1.U,
      (io.inst === LA32R.RDCNTID) -> rj,
    ),
  )

  val is_csr  = func_type === FuncType.csr
  val is_wr   = op_type === CsrOpType.wr
  val is_xchg = op_type === CsrOpType.xchg
  io.csr_reg_read.re    := is_csr
  io.csr_reg_read.raddr := Mux(io.inst === LA32R.RDCNTID, CSRCodes.TID, imm(13, 0))
  io.csr_iswf           := is_csr && (is_wr || is_xchg)
  io.csr_wfreg          := imm(13, 0)
  io.csr_wmask          := Mux(is_xchg, rj_value, ALL_MASK.U)
  io.csr_value := MateDefault(
    op_type,
    io.csr_reg_read.rdata,
    Seq(
      CsrOpType.cnth -> stable_counter.counter(63, 32),
      CsrOpType.cntl -> stable_counter.counter(31, 0),
    ),
  )

  io.exc_type := MuxCase(
    ECodes.NONE,
    List(
      (is_none && io.pc =/= 0.U)             -> ECodes.INE, // inst does not exist and is not caused by flush
      (is_exc && op_type === ExcOpType.brk)  -> ECodes.BRK,
      (is_exc && op_type === ExcOpType.sys)  -> ECodes.SYS,
      (is_exc && op_type === ExcOpType.ertn) -> ECodes.ertn,
    ),
  )
}
