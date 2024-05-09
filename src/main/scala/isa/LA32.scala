package isa

import chisel3._
import chisel3.util._

trait InstType {  // 指令类型
    def Inst2R      = "b0010".U
    def Inst3R      = "b0011".U
    def Inst4R      = "b0100".U

    def Inst2RI8    = "b0000".U
    def Inst2RI12   = "b0001".U //至少ori的立即数需要零拓展
    def Inst2RI14   = "b0101".U
    def Inst2RI16   = "b0110".U
    def Inst2RI20   = "b0111".U // add
    def Inst2RI26   = "b1000".U // add
    def Inst2RUI5   = "b1001".U // add
    def Inst2RUI6   = "b1010".U // add

    def Inst1RI21   = "b1011".U
    def Inst0Rcode  = "b1100".U //add
    def InstI26     = "b1101".U
    def InstCSR14   = "b1110".U //add

    // def IsWriteReg(instType: UInt): Bool = !instType(4)  // 是否写寄存器
    // def OffWhich(instType: UInt): UInt = instType(7, 6)  // 偏移的形式地址是什么类型
    def apply() = UInt(7.W) 
}

object OffType {
    def off_rj_or_direct = "b01".U
    def off_pc           = "b10".U
    def apply() = UInt(2.W)
}

object FuncType {  //功能类型
    def bru     = "b0000".U
    def alu     = "b0001".U
    def mem     = "b0010".U
    def div     = "b0011".U
    def mul     = "b0100".U
    def nondiv  = "b0101".U
    def nonmul  = "b0110".U
    def csr     = "b0111".U
    def exc     = "b1000".U
    def apply() = UInt(4.W)
}

object BruOptype {
    def b       = "b000".U
    def beq     = "b001".U
    def bne     = "b010".U
    def blt     = "b011".U
    def bge     = "b100".U
    def bltu    = "b101".U
    def bgeu    = "b110".U
    def apply() = UInt(3.W)
}

object AluOpType {
    def non     = "b000000".U
    def add     = "b100000".U//防止匹配到分支的matedefault的Bruoptype和其他匹配问题
    def sub     = "b100010".U
    def slt     = "b100011".U
    def sltu    = "b100100".U
    def and     = "b100101".U
    def nor     = "b100110".U
    def or      = "b100111".U
    def xor     = "b101000".U
    def sll     = "b101001".U
    def srl     = "b101010".U
    def sra     = "b101011".U
    def lui     = "b101100".U
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
    def readw     = "b1100".U
    
    def readh     = "b1011".U
    def readhu    = "b1010".U

    def readb     = "b1001".U
    def readbu    = "b1000".U

    def h         = "b01".U
    def b         = "b00".U

    def writew    = "b0000".U
    def writeh    = "b0001".U
    def writeb    = "b0010".U
    def isread(memOpType: UInt): Bool = memOpType(3)
    def apply()  = UInt(4.W)
}

object CsrOpType {
    def rd    = "b000".U
    def wr    = "b001".U
    def xchg  = "b010".U
    def cntrd = "b011".U
    def cnth  = "b100".U
    def cntl  = "b101".U
    def apply() = UInt(2.W)
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

object ExcOpType {
    def sys  = "b00".U
    def ertn = "b01".U
    def brk  = "b10".U
    def apply() = UInt(2.W)
}

object LA32 extends InstType {
    // rdcnt
    def RDCNTID     = BitPat("b0000000000000000011000?????00000")
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
    def CACOP       = BitPat("b0000011000??????????????????????") // 尚未实现

    // tlb
    def TLBSRCH     = BitPat("b00000110010010000010100000000000") // 尚未实现
    def TLBRD       = BitPat("b00000110010010000010110000000000") // 尚未实现
    def TLBWR       = BitPat("b00000110010010000011000000000000") // 尚未实现
    def TLBFILL     = BitPat("b00000110010010000011010000000000") // 尚未实现

    // priv
    def ERTN        = BitPat("b00000110010010000011100000000000")
    def IDLE        = BitPat("b00000110010010001???????????????") // 尚未实现
    def INVTLB      = BitPat("b00000110010010011???????????????") // 尚未实现

    // imm and pc
    def LU12I_W     = BitPat("b0001010?????????????????????????")
    def PCADDU12I   = BitPat("b0001110?????????????????????????")

    // atmomic
    def LL_W        = BitPat("b00100000????????????????????????") // 尚未实现
    def SC_W        = BitPat("b00100001????????????????????????") // 尚未实现

    // load-store
    def LD_B        = BitPat("b0010100000??????????????????????")
    def LD_H        = BitPat("b0010100001??????????????????????")
    def LD_W        = BitPat("b0010100010??????????????????????")
    def ST_B        = BitPat("b0010100100??????????????????????")
    def ST_H        = BitPat("b0010100101??????????????????????")
    def ST_W        = BitPat("b0010100110??????????????????????")
    def LD_BU       = BitPat("b0010101000??????????????????????")
    def LD_HU       = BitPat("b0010101001??????????????????????")
    
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
        LD_W      -> List(Inst2RI12,   FuncType.mem,   MemOpType.readw,     IsWf.y,   SrcType.rj,   SrcType.imm     ),
        LD_H      -> List(Inst2RI12,   FuncType.mem,   MemOpType.readh,     IsWf.y,   SrcType.rj,   SrcType.imm     ),
        LD_B      -> List(Inst2RI12,   FuncType.mem,   MemOpType.readb,     IsWf.y,   SrcType.rj,   SrcType.imm     ),
        LD_BU     -> List(Inst2RI12,   FuncType.mem,   MemOpType.readbu,    IsWf.y,   SrcType.rj,   SrcType.imm     ),
        LD_HU     -> List(Inst2RI12,   FuncType.mem,   MemOpType.readhu,    IsWf.y,   SrcType.rj,   SrcType.imm     ),
        ST_W      -> List(Inst2RI12,   FuncType.mem,   MemOpType.writew,    IsWf.n,   SrcType.rj,   SrcType.rd_imm  ),
        ST_H      -> List(Inst2RI12,   FuncType.mem,   MemOpType.writeh,    IsWf.n,   SrcType.rj,   SrcType.rd_imm  ),
        ST_B      -> List(Inst2RI12,   FuncType.mem,   MemOpType.writeb,    IsWf.n,   SrcType.rj,   SrcType.rd_imm  ),
        JIRL      -> List(Inst2RI16,   FuncType.bru,   AluOpType.add,       IsWf.y,   SrcType.pc,   SrcType.is4     ),
        B         -> List(Inst2RI26,   FuncType.bru,   BruOptype.b,         IsWf.n,   SrcType.rj,   SrcType.rk      ),
        BL        -> List(Inst2RI26,   FuncType.bru,   AluOpType.add,       IsWf.y,   SrcType.pc,   SrcType.is4     ),
        BEQ       -> List(Inst2RI16,   FuncType.bru,   BruOptype.beq,       IsWf.n,   SrcType.rj,   SrcType.rd      ),
        BNE       -> List(Inst2RI16,   FuncType.bru,   BruOptype.bne,       IsWf.n,   SrcType.rj,   SrcType.rd      ),
        BLT       -> List(Inst2RI16,   FuncType.bru,   BruOptype.blt,       IsWf.n,   SrcType.rj,   SrcType.rd      ),
        BLTU      -> List(Inst2RI16,   FuncType.bru,   BruOptype.bltu,      IsWf.n,   SrcType.rj,   SrcType.rd      ),
        BGE       -> List(Inst2RI16,   FuncType.bru,   BruOptype.bge,       IsWf.n,   SrcType.rj,   SrcType.rd      ),
        BGEU      -> List(Inst2RI16,   FuncType.bru,   BruOptype.bgeu,      IsWf.n,   SrcType.rj,   SrcType.rd      ),
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
        SYSCALL   -> List(Inst0Rcode,  FuncType.exc,   ExcOpType.sys,       IsWf.n,   SrcType.is4,  SrcType.is4     ),
        BREAK     -> List(Inst0Rcode,  FuncType.exc,   ExcOpType.brk,       IsWf.n,   SrcType.is4,  SrcType.is4     ),
        CSRRD     -> List(InstCSR14,   FuncType.csr,   CsrOpType.rd,        IsWf.y,   SrcType.rj,   SrcType.rd      ),
        CSRWR     -> List(InstCSR14,   FuncType.csr,   CsrOpType.wr,        IsWf.y,   SrcType.rj,   SrcType.rd      ),
        CSRXCHG   -> List(InstCSR14,   FuncType.csr,   CsrOpType.xchg,      IsWf.y,   SrcType.rj,   SrcType.rd      ),
        ERTN      -> List(Inst3R,      FuncType.exc,   ExcOpType.ertn,      IsWf.n,   SrcType.is4,  SrcType.is4     ),

        // rdcnt
        RDCNTID   -> List(Inst2R,      FuncType.csr,   CsrOpType.cntrd,     IsWf.y,   SrcType.rj,   SrcType.rj      ),
        RDCNTVHW  -> List(Inst2R,      FuncType.csr,   CsrOpType.cnth,      IsWf.y,   SrcType.rd,   SrcType.rd      ),
        RDCNTVLW  -> List(Inst2R,      FuncType.csr,   CsrOpType.cntl,      IsWf.y,   SrcType.rd,   SrcType.rd      ),
    )
}