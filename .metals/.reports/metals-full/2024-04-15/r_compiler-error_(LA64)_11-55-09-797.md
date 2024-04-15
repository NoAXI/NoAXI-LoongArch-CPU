file://<WORKSPACE>/src/main/scala/stages/ID.scala
### java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.13.8
Classpath:
<WORKSPACE>/.bloop/out/LA64/bloop-bsp-clients-classes/classes-Metals-8zbWxOTwR8el2lLpy5I0-w== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.9.9/semanticdb-javac-0.9.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/6.2.0/chisel_2.13-6.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.8/scala-library-2.13.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.8/scala-reflect-2.13.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/net/jcazevedo/moultingyaml_2.13/0.4.2/moultingyaml_2.13-0.4.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.6/json4s-native_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.10.0/commons-text-1.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.6/data-class_2.13-0.2.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.9.2/os-lib_2.13-0.9.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-parallel-collections_2.13/1.0.4/scala-parallel-collections_2.13-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.1.0/upickle_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/1.3.0/firtool-resolver_2.13-1.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/nscala-time/nscala-time_2.13/2.22.0/nscala-time_2.13-2.22.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/yaml/snakeyaml/1.26/snakeyaml-1.26.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.6/json4s-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.6/json4s-native-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.0.0/geny_2.13-1.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.1.0/ujson_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.1.0/upack_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.1.0/upickle-implicits_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/dev/dirs/directories/26/directories-26.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/outr/scribe_2.13/3.13.0/scribe_2.13-3.13.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/coursier_2.13/2.1.8/coursier_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/joda-time/joda-time/2.10.1/joda-time-2.10.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/joda/joda-convert/2.2.0/joda-convert-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.6/json4s-ast_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.6/json4s-scalap_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.1.0/upickle-core_2.13-3.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/outr/perfolation_2.13/1.2.9/perfolation_2.13-1.2.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.13/0.3.1/sourcecode_2.13-0.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/outr/moduload_2.13/1.1.7/moduload_2.13-1.1.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/plokhotnyuk/jsoniter-scala/jsoniter-scala-core_2.13/2.13.5.2/jsoniter-scala-core_2.13-2.13.5.2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/coursier-core_2.13/2.1.8/coursier-core_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/coursier-cache_2.13/2.1.8/coursier-cache_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/coursier-proxy-setup/2.1.8/coursier-proxy-setup-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/concurrent-reference-hash-map/1.1.0/concurrent-reference-hash-map-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/coursier-util_2.13/2.1.8/coursier-util_2.13-2.1.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/jniutils/windows-jni-utils/0.3.3/windows-jni-utils-0.3.3.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-archiver/4.9.0/plexus-archiver-4.9.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-container-default/2.1.1/plexus-container-default-2.1.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/virtuslab/scala-cli/config_2.13/0.2.1/config_2.13-0.2.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/windows-ansi/windows-ansi/0.0.5/windows-ansi-0.0.5.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/javax/inject/javax.inject/1/javax.inject-1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-utils/4.0.0/plexus-utils-4.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-io/3.4.1/plexus-io-3.4.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/commons-io/commons-io/2.15.0/commons-io-2.15.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-compress/1.24.0/commons-compress-1.24.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/iq80/snappy/snappy/0.4/snappy-0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/tukaani/xz/1.9/xz-1.9.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/luben/zstd-jni/1.5.5-10/zstd-jni-1.5.5-10.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/codehaus/plexus/plexus-classworlds/2.6.0/plexus-classworlds-2.6.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/xbean/xbean-reflect/3.7/xbean-reflect-3.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/fusesource/jansi/jansi/1.18/jansi-1.18.jar [exists ]
Options:
-language:reflectiveCalls -deprecation -feature -Xcheckinit -Yrangepos -Xplugin-require:semanticdb


action parameters:
uri: file://<WORKSPACE>/src/main/scala/stages/ID.scala
text:
```scala
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

object ALUOpType {
    def add     = "b100000".U
    def sub     = "b100001".U
    def slt     = "b100010".U
    def sltu    = "b100011".U
    def and     = "b100100".U
    def nor     = "b100101".U
    def or      = "b100110".U
    def xor     = "b100111".U
    def sll     = "b101000".U
    def srl     = "b101001".U
    def sra     = "b101010".U
    def lui     = "b101011".U
}

object decOpType {
    def branch  = "b0".U
}

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

    val table = Array (
        ADD_W       -> List(Inst3R,      FuncType.alu,   ALUOpType.add      ),
        // ADD_D       -> List(Inst2R, FuncType.alu, ALUOpType.add),
        SUB_W       -> List(Inst3R,      FuncType.alu,   ALUOpType.sub      ),
        // SUB_D       -> List(Inst2R, FuncType.alu, ALUOpType.sub),
        SLT         -> List(Inst3R,      FuncType.alu,   ALUOpType.slt      ),     
        SLTU        -> List(Inst3R,      FuncType.alu,   ALUOpType.sltu     ),
        NOR         -> List(Inst3R,      FuncType.alu,   ALUOpType.nor      ),
        AND         -> List(Inst3R,      FuncType.alu,   ALUOpType.and      ),
        OR          -> List(Inst3R,      FuncType.alu,   ALUOpType.or       ),
        XOR         -> List(Inst3R,      FuncType.alu,   ALUOpType.xor      ),
        SLLI_W      -> List(Inst2RUI5,   FuncType.alu,   ALUOpType.sll      ),
        SRLI_W      -> List(Inst2RUI5,   FuncType.alu,   ALUOpType.srl      ),
        SRAI_W      -> List(Inst2RUI5,   FuncType.alu,   ALUOpType.sra      ),
        ADDI_W      -> List(Inst2RI12,   FuncType.alu,   ALUOpType.add      ),
        LD_W        -> List(Inst2RI12,   FuncType.alu,   ALUOpType.add      ),
        ST_W        -> List(Inst2RI12,   FuncType.alu,   ALUOpType.add      ),
        JIRL        -> List(Inst2RI16,   FuncType.alu,   ALUOpType.add      ),
        B           -> List(Inst2RI26,   FuncType.dec,   decOpType.branch   ),
        BL          -> List(Inst2RI26,   FuncType.alu,   ALUOpType.add      ),
        BEQ         -> List(Inst2RI16,   FuncType.dec,   decOpType.branch   ),
        BNE         -> List(Inst2RI16,   FuncType.dec,   decOpType.branch   ),
        LU12I_W     -> List(Inst2RI20,   FuncType.alu,   ALUOpType.lui      )
    )
    //assign src2_is_4  =  inst_jirl | inst_bl;
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

    val fs_to_ds_bus_r = RegInit(0.U(FS_TO_DS_BUS_WIDTH.W))

    val rd   = ds_inst( 4,  0)
    val rj   = ds_inst( 9,  5)
    val rk   = ds_inst(14, 10)

    val i12  = ds_inst(21, 10)
    val i20  = ds_inst(24,  5)
    val i16  = ds_inst(25, 10)
    val i26  = Cat(ds_inst(9, 0), ds_inst(25, 10))

    val List(instType, funcType, aluOpType) = 
        ListLookup(ds_inst, List(Inst3R, FuncType.alu, ALUOpType.add), LA64_ALUInst.table)

    val imm = MateDefault(instType, 4.U, List(
        Inst2RI12 -> SignedExtend(i12, DATA_WIDTH_W),
        // Inst2RI16 -> SignedExtend(i16, DATA_WIDTH_W),
        Inst2RI20 -> Cat(i20, Fill(12, 0.U)),
        // Inst2RI26 -> Cat(Fill(38, i26(25)), i26),
        Inst2RUI5 -> rk,
    ))

    val br_offs = Mux(instType === Inst2RI26, SignedExtend(Cat(i26, Fill(2, 0.U)), 32), 
                                              SignedExtend(Cat(i16, Fill(2, 0.U)), 32))

    val jirl_offs = SignedExtend(Cat(i16, Fill(2, 0.U)), 32)

    val src_reg_is_rd = (ds_inst === LA64_ALUInst.BEQ 
 ds_inst === LA64_ALUInst.BNE 
 ds_inst === LA64_ALUInst.ST_W)

    val src1_is_pc = (ds_inst === LA64_ALUInst.JIRL
 ds_inst === LA64_ALUInst.BL)

    val src2_is_imm = (ds_inst === LA64_ALUInst.SLLI_W
 ds_inst === LA64_ALUInst.SRLI_W
 ds_inst === LA64_ALUInst.SRAI_W
 ds_inst === LA64_ALUInst.ADDI_W
 ds_inst === LA64_ALUInst.LD_W
 ds_inst === LA64_ALUInst.ST_W
 ds_inst === LA64_ALUInst.LU12I_W
 ds_inst === LA64_ALUInst.JIRL
 ds_inst === LA64_ALUInst.BL)

    val res_from_mem = (ds_inst === LA64_ALUInst.LD_W)
    val dst_is_r1    = (ds_inst === LA64_ALUInst.BL)
    val gr_we        = (ds_inst =/= LA64_ALUInst.ST_W &&
                        ds_inst =/= LA64_ALUInst.BEQ && 
                        ds_inst =/= LA64_ALUInst.BNE &&
                        ds_inst =/= LA64_ALUInst.B)
    val mem_we       = (ds_inst === LA64_ALUInst.ST_W)
    val dest         = Mux(dst_is_r1, 1.U, rd)
    val rf_raddr1 = rj;
    val rf_raddr2 = Mux(src_reg_is_rd, rd, rk)

    val u_regfile = Module(new REG)
    u_regfile.io.rf_raddr1 := rf_raddr1
    u_regfile.io.rf_raddr2 := rf_raddr2
    u_

    val (
        rf_we  ,
        rf_waddr,
        rf_wdata
    )=(
        io.ws_to_rf_bus(37),
        io.ws_to_rf_bus(36, 32),
        io.ws_to_rf_bus(31, 0)
    ) 

    val (
        ds_inst,
        ds_pc,
    )=(
        fs_to_ds_bus_r(63, 32),
        fs_to_ds_bus_r(31, 0),
    ) 
    
    val io.ds_to_es_bus = Cat(alu_op       ,   // 12
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
```



#### Error stacktrace:

```
scala.collection.mutable.ArrayBuffer.apply(ArrayBuffer.scala:98)
	scala.reflect.internal.Types$Type.findMemberInternal$1(Types.scala:1030)
	scala.reflect.internal.Types$Type.findMember(Types.scala:1035)
	scala.reflect.internal.Types$Type.memberBasedOnName(Types.scala:661)
	scala.reflect.internal.Types$Type.nonLocalMember(Types.scala:652)
	scala.tools.nsc.typechecker.Typers$Typer.member(Typers.scala:669)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typed1$57(Typers.scala:5259)
	scala.tools.nsc.typechecker.Typers$Typer.typedSelect$1(Typers.scala:5259)
	scala.tools.nsc.typechecker.Typers$Typer.typedSelectOrSuperCall$1(Typers.scala:5411)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:5986)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6041)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$adapt$12(Typers.scala:1024)
	scala.tools.nsc.typechecker.Typers$Typer.silent(Typers.scala:712)
	scala.tools.nsc.typechecker.Typers$Typer.insertApply$1(Typers.scala:4523)
	scala.tools.nsc.typechecker.Typers$Typer.vanillaAdapt$1(Typers.scala:1213)
	scala.tools.nsc.typechecker.Typers$Typer.adapt(Typers.scala:1278)
	scala.tools.nsc.typechecker.Typers$Typer.adapt(Typers.scala:1251)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6056)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typed1$41(Typers.scala:5074)
	scala.tools.nsc.typechecker.Typers$Typer.silent(Typers.scala:698)
	scala.tools.nsc.typechecker.Typers$Typer.normalTypedApply$1(Typers.scala:5076)
	scala.tools.nsc.typechecker.Typers$Typer.typedApply$1(Typers.scala:5104)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:5985)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6041)
	scala.tools.nsc.typechecker.Typers$Typer.computeType(Typers.scala:6130)
	scala.tools.nsc.typechecker.Namers$Namer.assignTypeToTree(Namers.scala:1127)
	scala.tools.nsc.typechecker.Namers$Namer.valDefSig(Namers.scala:1745)
	scala.tools.nsc.typechecker.Namers$Namer.memberSig(Namers.scala:1930)
	scala.tools.nsc.typechecker.Namers$Namer.typeSig(Namers.scala:1880)
	scala.tools.nsc.typechecker.Namers$Namer$ValTypeCompleter.completeImpl(Namers.scala:944)
	scala.tools.nsc.typechecker.Namers$LockingTypeCompleter.complete(Namers.scala:2078)
	scala.tools.nsc.typechecker.Namers$LockingTypeCompleter.complete$(Namers.scala:2076)
	scala.tools.nsc.typechecker.Namers$TypeCompleterBase.complete(Namers.scala:2071)
	scala.reflect.internal.Symbols$Symbol.completeInfo(Symbols.scala:1561)
	scala.reflect.internal.Symbols$Symbol.info(Symbols.scala:1533)
	scala.reflect.internal.Symbols$Symbol.initialize(Symbols.scala:1722)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:5625)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6041)
	scala.tools.nsc.typechecker.Typers$Typer.typedStat$1(Typers.scala:6119)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typedStats$8(Typers.scala:3410)
	scala.tools.nsc.typechecker.Typers$Typer.typedStats(Typers.scala:3410)
	scala.tools.nsc.typechecker.Typers$Typer.typedTemplate(Typers.scala:2064)
	scala.tools.nsc.typechecker.Typers$Typer.typedClassDef(Typers.scala:1895)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:5951)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6041)
	scala.tools.nsc.typechecker.Typers$Typer.typedStat$1(Typers.scala:6119)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typedStats$8(Typers.scala:3410)
	scala.tools.nsc.typechecker.Typers$Typer.typedStats(Typers.scala:3410)
	scala.tools.nsc.typechecker.Typers$Typer.typedPackageDef$1(Typers.scala:5634)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:5954)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6041)
	scala.tools.nsc.typechecker.Analyzer$typerFactory$TyperPhase.apply(Analyzer.scala:117)
	scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:459)
	scala.tools.nsc.interactive.Global$TyperRun.applyPhase(Global.scala:1349)
	scala.tools.nsc.interactive.Global$TyperRun.typeCheck(Global.scala:1342)
	scala.tools.nsc.interactive.Global.typeCheck(Global.scala:680)
	scala.meta.internal.pc.PcCollector.<init>(PcCollector.scala:29)
	scala.meta.internal.pc.PcSemanticTokensProvider$Collector$.<init>(PcSemanticTokensProvider.scala:19)
	scala.meta.internal.pc.PcSemanticTokensProvider.Collector$lzycompute$1(PcSemanticTokensProvider.scala:19)
	scala.meta.internal.pc.PcSemanticTokensProvider.Collector(PcSemanticTokensProvider.scala:19)
	scala.meta.internal.pc.PcSemanticTokensProvider.provide(PcSemanticTokensProvider.scala:73)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$semanticTokens$1(ScalaPresentationCompiler.scala:169)
```
#### Short summary: 

java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)