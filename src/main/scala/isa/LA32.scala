package isa

import chisel3._
import chisel3.util._

trait InstType {  // 指令类型
    def Inst2R      = "b00_00_0010".U
    def Inst3R      = "b00_00_0011".U
    def Inst4R      = "b00_00_0100".U

    def Inst2RI8    = "b00_00_0000".U
    def Inst2RI12   = "b00_00_0001".U //至少ori的立即数需要零拓展
    def Inst2RI14   = "b00_00_0101".U
    def Inst2RI16   = "b01_00_0110".U
    def Inst2RI20   = "b00_00_0111".U // add
    def Inst2RI26   = "b10_00_1000".U // add
    def Inst2RUI5   = "b00_00_1001".U // add
    def Inst2RUI6   = "b00_00_1010".U // add

    def Inst1RI21   = "b00_01_0000".U
    def Inst1RCSR   = "b00_01_0001".U //add
    def InstI26     = "b00_01_0010".U

    // def IsWriteReg(instType: UInt): Bool = !instType(4)  // 是否写寄存器
    def OffWhich(instType: UInt): UInt = instType(7, 6)  // 偏移的形式地址是什么类型
    def apply() = UInt(7.W) 
}

object OffType {
    def off_rj_or_direct = "b01".U
    def off_pc           = "b10".U
    def apply() = UInt(2.W)
}

object FuncType {  //功能类型
    def non     = "b000".U
    def alu     = "b001".U
    def mem     = "b010".U
    def div     = "b011".U
    def mul     = "b100".U
    def nondiv  = "b101".U
    def nonmul  = "b110".U
    def apply() = UInt(3.W)
}

object AluOpType {
    def non     = "b000000".U
    def add     = "b000001".U
    def sub     = "b000010".U
    def slt     = "b000011".U
    def sltu    = "b000100".U
    def and     = "b000101".U
    def nor     = "b000110".U
    def or      = "b000111".U
    def xor     = "b001000".U
    def sll     = "b001001".U
    def srl     = "b001010".U
    def sra     = "b001011".U
    def lui     = "b001100".U
    def apply() = UInt(6.W)
}

object DivOpType {
    def umod  = "b00".U
    def u     = "b10".U
    def smod  = "b01".U
    def s     = "b11".U
    def signed(divOpType: UInt): Bool = divOpType(0).asBool
    def apply() = UInt(2.W)
}

object MulOpType {
    def slow    = "b00".U
    def ulow    = "b01".U
    def shigh   = "b10".U
    def uhigh   = "b11".U
    def apply() = UInt(2.W) 
}

object MemOpType {
    def read    = "b0".U
    def write   = "b1".U
    def apply() = UInt(1.W)
}

object DecOpType {
    def branch  = "b0".U
    def jump    = "b1".U
    def apply() = UInt(1.W)
}

object IsWf {
    def y = "b1".U
    def n = "b0".U
    def apply() = UInt(1.W)
}

object SrcType {
    def is4     = "b000".U
    def rj      = "b001".U
    def rk      = "b010".U
    def rd      = "b011".U
    def pc      = "b100".U
    def imm     = "b101".U
    def immu    = "b110".U
    def rd_imm  = "b111".U
    def apply() = UInt(3.W)
}

object LA32 extends InstType {
    // rdcnt
    def RDCNTIDW    = BitPat("b0000000000000000011000?????00000")
    def RDCNTVLW    = BitPat("b000000000000000001100000000?????")
    def RDCNTVHW    = BitPat("b000000000000000001100100000?????")
    
    // logic reg-reg
    def ADD_W       = BitPat("b00000000000100000???????????????")
    def SUB_W       = BitPat("b00000000000100010???????????????")
    def SLT         = BitPat("b00000000000100100???????????????")
    def SLTU        = BitPat("b00000000000100101???????????????")
    def NOR         = BitPat("b00000000000101000???????????????")
    def AND         = BitPat("b00000000000101001???????????????")
    def OR          = BitPat("b00000000000101010???????????????")
    def XOR         = BitPat("b00000000000101011???????????????")
    def SLL_W       = BitPat("b00000000000101110???????????????")
    def SRL_W       = BitPat("b00000000000101111???????????????")
    def SRA_W       = BitPat("b00000000000110000???????????????")
    def MUL_W       = BitPat("b00000000000111000???????????????")
    def MULH_W      = BitPat("b00000000000111001???????????????")
    def MULH_WU     = BitPat("b00000000000111010???????????????")
    def DIV_W       = BitPat("b00000000001000000???????????????")
    def MOD_W       = BitPat("b00000000001000001???????????????")
    def DIV_WU      = BitPat("b00000000001000010???????????????")
    def MOD_WU      = BitPat("b00000000001000011???????????????")

    // else
    def BREAK       = BitPat("b00000000001010100???????????????")
    def SYSCALL     = BitPat("b00000000001010110???????????????")

    // logic reg-imm
    def SLLI_W      = BitPat("b00000000010000001???????????????")
    def SRLI_W      = BitPat("b00000000010001001???????????????")
    def SRAI_W      = BitPat("b00000000010010001???????????????")
    def SLTI        = BitPat("b0000001000??????????????????????")
    def SLTUI       = BitPat("b0000001001??????????????????????")
    def ADDI_W      = BitPat("b0000001010??????????????????????")
    def ANDI        = BitPat("b0000001101??????????????????????")
    def ORI         = BitPat("b0000001110??????????????????????")
    def XORI        = BitPat("b0000001111??????????????????????")

    // CSR
    def CSRRD       = BitPat("b00000100??????????????00000?????")
    def CSRWR       = BitPat("b00000100??????????????00001?????")
    def CSRXCHG     = BitPat("b00000100????????????????????????")

    // cacop
    def CACOP       = BitPat("b0000011000??????????????????????")

    // tlb
    def TLBSRCH     = BitPat("b00000110010010000010100000000000")
    def TLBRD       = BitPat("b00000110010010000010110000000000")
    def TLBWR       = BitPat("b00000110010010000011000000000000")
    def TLBFILL     = BitPat("b00000110010010000011010000000000")

    // priv
    def ERTN        = BitPat("b00000110010010000011100000000000")
    def IDLE        = BitPat("b00000110010010001???????????????")
    def INVTLB      = BitPat("b00000110010010011???????????????")

    // imm and pc
    def LU12I_W     = BitPat("b0001010?????????????????????????")
    def PCADDU12I   = BitPat("b0001110?????????????????????????")

    // atmomic
    def LLW         = BitPat("b00100000????????????????????????")
    def SCW         = BitPat("b00100001????????????????????????")

    // load-store
    def LDB         = BitPat("b0010100000??????????????????????")
    def LDH         = BitPat("b0010100001??????????????????????")
    def LD_W        = BitPat("b0010100010??????????????????????")
    def STB         = BitPat("b0010100100??????????????????????")
    def STH         = BitPat("b0010100101??????????????????????")
    def ST_W        = BitPat("b0010100110??????????????????????")
    def LDBU        = BitPat("b0010101000??????????????????????")
    def LDHU        = BitPat("b0010101001??????????????????????")
    
    // branch
    def JIRL        = BitPat("b010011??????????????????????????")
    def B           = BitPat("b010100??????????????????????????")
    def BL          = BitPat("b010101??????????????????????????")
    def BEQ         = BitPat("b010110??????????????????????????")
    def BNE         = BitPat("b010111??????????????????????????")    
    def BLT         = BitPat("b011000??????????????????????????")
    def BGE         = BitPat("b011001??????????????????????????")
    def BLTU        = BitPat("b011010??????????????????????????")
    def BGEU        = BitPat("b011011??????????????????????????")

    val table = Array ( 
        ADD_W     -> List(Inst3R,      FuncType.alu,   AluOpType.add,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        SUB_W     -> List(Inst3R,      FuncType.alu,   AluOpType.sub,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        SLT       -> List(Inst3R,      FuncType.alu,   AluOpType.slt,       IsWf.y,   SrcType.rj,   SrcType.rk      ),   
        SLTU      -> List(Inst3R,      FuncType.alu,   AluOpType.sltu,      IsWf.y,   SrcType.rj,   SrcType.rk      ),
        NOR       -> List(Inst3R,      FuncType.alu,   AluOpType.nor,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        AND       -> List(Inst3R,      FuncType.alu,   AluOpType.and,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        ANDI      -> List(Inst2RI12,   FuncType.alu,   AluOpType.and,       IsWf.y,   SrcType.rj,   SrcType.immu    ),
        OR        -> List(Inst3R,      FuncType.alu,   AluOpType.or,        IsWf.y,   SrcType.rj,   SrcType.rk      ),
        ORI       -> List(Inst2RI12,   FuncType.alu,   AluOpType.or,        IsWf.y,   SrcType.rj,   SrcType.immu    ),
        XOR       -> List(Inst3R,      FuncType.alu,   AluOpType.xor,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        XORI      -> List(Inst2RI12,   FuncType.alu,   AluOpType.xor,       IsWf.y,   SrcType.rj,   SrcType.immu    ),
        SLL_W     -> List(Inst3R,      FuncType.alu,   AluOpType.sll,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        SLLI_W    -> List(Inst2RUI5,   FuncType.alu,   AluOpType.sll,       IsWf.y,   SrcType.rj,   SrcType.imm     ),
        SRL_W     -> List(Inst3R,      FuncType.alu,   AluOpType.srl,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        SRLI_W    -> List(Inst2RUI5,   FuncType.alu,   AluOpType.srl,       IsWf.y,   SrcType.rj,   SrcType.imm     ),
        SRA_W     -> List(Inst3R,      FuncType.alu,   AluOpType.sra,       IsWf.y,   SrcType.rj,   SrcType.rk      ),
        SRAI_W    -> List(Inst2RUI5,   FuncType.alu,   AluOpType.sra,       IsWf.y,   SrcType.rj,   SrcType.imm     ),
        ADDI_W    -> List(Inst2RI12,   FuncType.alu,   AluOpType.add,       IsWf.y,   SrcType.rj,   SrcType.imm     ),
        LD_W      -> List(Inst2RI12,   FuncType.mem,   MemOpType.read,      IsWf.y,   SrcType.rj,   SrcType.imm     ),
        ST_W      -> List(Inst2RI12,   FuncType.mem,   MemOpType.write,     IsWf.n,   SrcType.rj,   SrcType.rd_imm  ),
        JIRL      -> List(Inst2RI16,   FuncType.alu,   AluOpType.add,       IsWf.y,   SrcType.pc,   SrcType.is4     ),
        B         -> List(Inst2RI26,   FuncType.non,   AluOpType.non,       IsWf.n,   SrcType.rj,   SrcType.rk      ),
        BL        -> List(Inst2RI26,   FuncType.alu,   AluOpType.add,       IsWf.y,   SrcType.pc,   SrcType.is4     ),
        BEQ       -> List(Inst2RI16,   FuncType.non,   AluOpType.non,       IsWf.n,   SrcType.rj,   SrcType.rd      ),
        BNE       -> List(Inst2RI16,   FuncType.non,   AluOpType.non,       IsWf.n,   SrcType.rj,   SrcType.rd      ),
        LU12I_W   -> List(Inst2RI20,   FuncType.alu,   AluOpType.lui,       IsWf.y,   SrcType.rj,   SrcType.imm     ),
        PCADDU12I -> List(Inst2RI20,   FuncType.alu,   AluOpType.add,       IsWf.y,   SrcType.pc,   SrcType.imm     ),
        SLTI      -> List(Inst2RI12,   FuncType.alu,   AluOpType.slt,       IsWf.y,   SrcType.rj,   SrcType.imm     ),
        SLTUI     -> List(Inst2RI12,   FuncType.alu,   AluOpType.sltu,      IsWf.y,   SrcType.rj,   SrcType.imm     ),
        DIV_W     -> List(Inst3R,      FuncType.div,   DivOpType.s,         IsWf.y,   SrcType.rj,   SrcType.rk      ),
        DIV_WU    -> List(Inst3R,      FuncType.div,   DivOpType.u,         IsWf.y,   SrcType.rj,   SrcType.rk      ),
        MUL_W     -> List(Inst3R,      FuncType.mul,   MulOpType.slow,      IsWf.y,   SrcType.rj,   SrcType.rk      ),
        MULH_W    -> List(Inst3R,      FuncType.mul,   MulOpType.shigh,     IsWf.y,   SrcType.rj,   SrcType.rk      ),
        MULH_WU   -> List(Inst3R,      FuncType.mul,   MulOpType.uhigh,     IsWf.y,   SrcType.rj,   SrcType.rk      ),
        MOD_W     -> List(Inst3R,      FuncType.div,   DivOpType.smod,      IsWf.y,   SrcType.rj,   SrcType.rk      ),
        MOD_WU    -> List(Inst3R,      FuncType.div,   DivOpType.umod,      IsWf.y,   SrcType.rj,   SrcType.rk      ),
    )
}