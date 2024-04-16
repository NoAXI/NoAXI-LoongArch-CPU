package stages

import chisel3._
import chisel3.util._

import config._
import config.Functions._

// 有rd的就是要写入寄存器吧
// 目前是乱编码的
trait InstType {  // 指令类型
    def Inst2R      = "b00010".U
    def Inst3R      = "b00011".U
    def Inst4R      = "b00100".U

    def Inst2RI8    = "b00000".U
    def Inst2RI12   = "b00001".U
    def Inst2RI14   = "b00101".U
    def Inst2RI16   = "b00110".U
    def Inst2RI20   = "b00111".U // add
    def Inst2RI26   = "b01000".U // add
    def Inst2RUI5   = "b01001".U // add
    def Inst2RUI6   = "b01010".U // add

    def Inst1RI21   = "b10000".U
    def InstI26     = "b10001".U

    def isWriteReg(instType: UInt): Bool = !instType(4)  // 是否写寄存器
    def apply() = UInt(4.W) 
}

object FuncType {  //功能类型
    def alu = "b0".U
    def dec = "b1".U
    def apply() = UInt(1.W)
}

object decOpType {
    def branch  = "b0".U
}

object Src1Type {
    def nor = "b0".U
    def pc  = "b1".U
}

object Src2Type {
    def nor = "b00".U
    def imm = "b01".U
    def is4 = "b10".U
}

//000000101011
//00000011

object LA64_ALUInst extends InstType with Parameters {
    def ADD_W       = BitPat("b00000000000100000???????????????")
    // def ADD_D       = BitPat("b00000000000100001???????????????")
    def SUB_W       = BitPat("b00000000000100010???????????????")
    // def SUB_D       = BitPat("b00000000000100011???????????????")
    def SLT         = BitPat("b00000000000100100???????????????")
    def SLTU        = BitPat("b00000000000100101???????????????")
    def NOR         = BitPat("b00000000000101000???????????????")
    def AND         = BitPat("b00000000000101001???????????????")
    def OR          = BitPat("b00000000000101010???????????????")
    def XOR         = BitPat("b00000000000101011???????????????")
    def SLLI_W      = BitPat("b00000000010000001???????????????")
    def SRLI_W      = BitPat("b00000000010001001???????????????") 
    def SRAI_W      = BitPat("b00000000010010001???????????????")
    def ADDI_W      = BitPat("b0000001010??????????????????????") 
    def LD_W        = BitPat("b0010100010??????????????????????")  
    def ST_W        = BitPat("b0010100110??????????????????????")   
    def JIRL        = BitPat("b001101??????????????????????????")   
    def B           = BitPat("b001110??????????????????????????")  
    def BL          = BitPat("b001111??????????????????????????")
    def BEQ         = BitPat("b010000??????????????????????????")    
    def BNE         = BitPat("b010001??????????????????????????")
    def LU12I_W     = BitPat("b0001010?????????????????????????")

    // def ADDI_W      = BitPat("b0000001010??????????????????????")
    // def ADDI_D      = BitPat("b0000001011??????????????????????")
    // def ADDU16I_D   = BitPat("b000100??????????????????????????")
    // def ALSL_W      = BitPat("b000000000000010?????????????????")
    // def ALSL_WU     = BitPat("b000000000000011?????????????????")
    // def ALSL_D      = BitPat("b00000000001011??????????????????")
    // def LU12I_W     = BitPat("b0001010?????????????????????????")
    // def LU32I_D     = BitPat("b0001011?????????????????????????")
    // def LU52I_D     = BitPat("b0000001100??????????????????????")

    //                    (instType,    funcType,       aluOpType,  ,src_addr2_is_rd,      src1,          src2  )
    val table = Array (
        ADD_W     -> List(Inst3R,      FuncType.alu,   ALUOpType.add      ,false.B,  Src1Type.nor, Src2Type.nor),
        // ADD_D     -> List(Inst2R, FuncType.alu, ALUOpType.add),      
        SUB_W     -> List(Inst3R,      FuncType.alu,   ALUOpType.sub      ,false.B,  Src1Type.nor, Src2Type.nor),
        // SUB_D     -> List(Inst2R, FuncType.alu, ALUOpType.sub),      
        SLT       -> List(Inst3R,      FuncType.alu,   ALUOpType.slt      ,false.B,  Src1Type.nor, Src2Type.nor),     
        SLTU      -> List(Inst3R,      FuncType.alu,   ALUOpType.sltu     ,false.B,  Src1Type.nor, Src2Type.nor),
        NOR       -> List(Inst3R,      FuncType.alu,   ALUOpType.nor      ,false.B,  Src1Type.nor, Src2Type.nor),
        AND       -> List(Inst3R,      FuncType.alu,   ALUOpType.and      ,false.B,  Src1Type.nor, Src2Type.nor),
        OR        -> List(Inst3R,      FuncType.alu,   ALUOpType.or       ,false.B,  Src1Type.nor, Src2Type.nor),
        XOR       -> List(Inst3R,      FuncType.alu,   ALUOpType.xor      ,false.B,  Src1Type.nor, Src2Type.nor),
        SLLI_W    -> List(Inst2RUI5,   FuncType.alu,   ALUOpType.sll      ,false.B,  Src1Type.nor, Src2Type.imm),
        SRLI_W    -> List(Inst2RUI5,   FuncType.alu,   ALUOpType.srl      ,false.B,  Src1Type.nor, Src2Type.imm),
        SRAI_W    -> List(Inst2RUI5,   FuncType.alu,   ALUOpType.sra      ,false.B,  Src1Type.nor, Src2Type.imm),
        ADDI_W    -> List(Inst2RI12,   FuncType.alu,   ALUOpType.add      ,false.B,  Src1Type.nor, Src2Type.imm),
        LD_W      -> List(Inst2RI12,   FuncType.alu,   ALUOpType.add      ,false.B,  Src1Type.nor, Src2Type.imm),
        ST_W      -> List(Inst2RI12,   FuncType.alu,   ALUOpType.add      ,true .B,  Src1Type.nor, Src2Type.imm),
        JIRL      -> List(Inst2RI16,   FuncType.alu,   ALUOpType.add      ,false.B,  Src1Type.pc , Src2Type.is4),
        B         -> List(Inst2RI26,   FuncType.dec,   decOpType.branch   ,false.B,  Src1Type.nor, Src2Type.nor),
        BL        -> List(Inst2RI26,   FuncType.alu,   ALUOpType.add      ,false.B,  Src1Type.pc , Src2Type.is4),
        BEQ       -> List(Inst2RI16,   FuncType.dec,   decOpType.branch   ,true .B,  Src1Type.nor, Src2Type.nor),
        BNE       -> List(Inst2RI16,   FuncType.dec,   decOpType.branch   ,true .B,  Src1Type.nor, Src2Type.nor),
        LU12I_W   -> List(Inst2RI20,   FuncType.alu,   ALUOpType.lui      ,false.B,  Src1Type.nor, Src2Type.imm)
    )
}

class ID_IO extends Bundle with Parameters {
    //allow in
    val es_allowin = Input(Bool())
    val ds_allowin = Output(Bool())

    //from fs
    val fs_to_ds_valid = Input(Bool()) 
    val fs_to_ds_bus = Input(UInt(FS_TO_DS_BUS_WIDTH.W)) 

    //** from writeback
    val ws_to_rf_bus = Input(UInt(WS_TO_RF_BUS_WIDTH.W))

    //to execute
    val ds_to_es_valid = Output(Bool()) 
    val ds_to_es_bus = Output(UInt(DS_TO_ES_BUS_WIDTH.W)) 

    //** to fs
    val br_bus = Output(UInt(BR_BUS_WIDTH.W))
}

class ID extends Module with Parameters with InstType{
    val io = IO(new ID_IO)

    //与上一流水级握手，获取上一流水级信息
    val fs_to_ds_bus_r = RegInit(0.U(FS_TO_DS_BUS_WIDTH.W))
    val ds_valid = RegInit(false.B)
    val ds_ready_go = true.B
    io.ds_allowin := !ds_valid || ds_ready_go && io.es_allowin
    io.ds_to_es_valid := ds_valid && ds_ready_go
    when (io.ds_allowin) {
        ds_valid := io.fs_to_ds_valid
    }
    when (io.fs_to_ds_valid && io.ds_allowin) {
        fs_to_ds_bus_r := io.fs_to_ds_bus
    }
    
    //提取上一流水级信息
    val ds_inst = fs_to_ds_bus_r(63, 32)
    val ds_pc = fs_to_ds_bus_r(31, 0)

    //提取指令基本信息
    val rd   = ds_inst( 4,  0)
    val rj   = ds_inst( 9,  5)
    val rk   = ds_inst(14, 10)
    val i12  = ds_inst(21, 10)
    val i20  = ds_inst(24,  5)
    val i16  = ds_inst(25, 10)
    val i26  = Cat(ds_inst(9, 0), ds_inst(25, 10))

    val List(instType, funcType, aluOpType, src_reg_is_rd, src1Type, src2Type) = 
        ListLookup(ds_inst, List("b11111".U, FuncType.alu, ALUOpType.add, false.B, Src1Type.nor, Src2Type.nor), LA64_ALUInst.table)

    val imm = MateDefault(instType, 4.U, List(
        Inst2RI12 -> SignedExtend(i12, 32),
        // Inst2RI16 -> SignedExtend(Cat(i16, Fill(2, 0.U)), 32), //br_offs (others) & jirl_offs
        Inst2RI20 -> Cat(i20, Fill(12, 0.U)),
        // Inst2RI26 -> SignedExtend(Cat(i26, Fill(2, 0.U)), 32), //br_offs (need_si26)
        Inst2RUI5 -> rk,
    ))

    val br_offs = Mux(instType === Inst2RI26, SignedExtend(Cat(i26, Fill(2, 0.U)), 32), 
                                              SignedExtend(Cat(i16, Fill(2, 0.U)), 32))

    val jirl_offs = SignedExtend(Cat(i16, Fill(2, 0.U)), 32)

    // val src_reg_is_rd = (ds_inst === LA64_ALUInst.BEQ 
    //                   || ds_inst === LA64_ALUInst.BNE 
    //                   || ds_inst === LA64_ALUInst.ST_W)

    // val src1_is_pc = (ds_inst === LA64_ALUInst.JIRL
    //                || ds_inst === LA64_ALUInst.BL)
    
    // val src2_is_imm = (ds_inst === LA64_ALUInst.SLLI_W
    //                 || ds_inst === LA64_ALUInst.SRLI_W
    //                 || ds_inst === LA64_ALUInst.SRAI_W
    //                 || ds_inst === LA64_ALUInst.ADDI_W
    //                 || ds_inst === LA64_ALUInst.LD_W
    //                 || ds_inst === LA64_ALUInst.ST_W
    //                 || ds_inst === LA64_ALUInst.LU12I_W
    //                 || ds_inst === LA64_ALUInst.JIRL
    //                 || ds_inst === LA64_ALUInst.BL)
    
    // val src2_is_4 = (ds_inst === LA64_ALUInst.JIRL
    //              || ds_inst === LA64_ALUInst.BL)

    val src1_is_pc = src1Type === Src1Type.pc
    val src2_is_4 = src2Type === Src2Type.is4
    val src2_is_imm = src2_is_4 | src2Type === Src2Type.imm

    val res_from_mem = (ds_inst === LA64_ALUInst.LD_W)
    val dst_is_r1    = (ds_inst === LA64_ALUInst.BL)
    val gr_we        = (ds_inst =/= LA64_ALUInst.ST_W &&
                        ds_inst =/= LA64_ALUInst.BEQ && 
                        ds_inst =/= LA64_ALUInst.BNE &&
                        ds_inst =/= LA64_ALUInst.B)
    val mem_we       = (ds_inst === LA64_ALUInst.ST_W)
    val dest         = Mux(dst_is_r1, 1.U, rd)

    val rf_raddr1 = rj;
    val rf_raddr2 = Mux(src_reg_is_rd === true.B, rd, rk)

    //提取ws传来的写回寄存器信息
    val rf_we = io.ws_to_rf_bus(37)
    val rf_waddr = io.ws_to_rf_bus(36, 32)
    val rf_wdata = io.ws_to_rf_bus(31, 0)

    //读寄存器 or 写寄存器
    val u_regfile = Module(new REG)
    u_regfile.io.raddr1 := rf_raddr1
    u_regfile.io.raddr2 := rf_raddr2
    u_regfile.io.we     := rf_we
    u_regfile.io.waddr  := rf_waddr
    u_regfile.io.wdata  := rf_wdata

    val rj_value  = u_regfile.io.rdata1
    val rkd_value = u_regfile.io.rdata2

    //写br_bus,传给fs
    val rj_eq_rd = (rj_value === rkd_value)
    val br_taken = (ds_inst === LA64_ALUInst.BEQ && rj_eq_rd
                 || ds_inst === LA64_ALUInst.BNE && !rj_eq_rd
                 || ds_inst === LA64_ALUInst.JIRL
                 || ds_inst === LA64_ALUInst.BL
                 || ds_inst === LA64_ALUInst.B) && ds_valid

    val br_target = Mux((ds_inst === LA64_ALUInst.BEQ
                      || ds_inst === LA64_ALUInst.BNE
                      || ds_inst === LA64_ALUInst.B
                      || ds_inst === LA64_ALUInst.BL), 
                        (ds_pc + br_offs),
                        (rj_value + jirl_offs))

    io.br_bus := Cat(br_taken, br_target)

    //传递信息
    val load_op = false.B // 它没用上
    io.ds_to_es_bus := Cat( aluOpType    ,   // 12 -> 6
                            load_op      ,   // 1
                            src1_is_pc   ,   // 1
                            src2_is_imm  ,   // 1
                            src2_is_4    ,   // 1
                            gr_we        ,   // 1
                            mem_we       ,   // 1
                            dest         ,   // 5
                            imm          ,   // 32
                            rj_value     ,   // 32
                            rkd_value    ,   // 32
                            ds_pc        ,    // 32
                            res_from_mem)
}

/*
val res = LookupTreeDefault(func(5, 0), adderRes, List(
    LSUOpType.amoswap -> src2,
    // LSUOpType.amoadd  -> adderRes,
    LSUOpType.amoxor  -> xorRes,
    LSUOpType.amoand  -> (src1 & src2),
    LSUOpType.amoor   -> (src1 | src2),
    LSUOpType.amomin  -> Mux(slt(0), src1, src2),
    LSUOpType.amomax  -> Mux(slt(0), src2, src1),
    LSUOpType.amominu -> Mux(sltu(0), src1, src2),
    LSUOpType.amomaxu -> Mux(sltu(0), src2, src1)
  )) 
 */