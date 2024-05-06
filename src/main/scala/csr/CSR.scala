package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

trait base {
  val id: UInt
  val info: Data
  def write(value: UInt) = {
    info := value.asTypeOf(info)
  }
}

class CRMD_info extends Bundle {
  val zero = UInt(22.W)
  val we   = Bool()    // 指令和数据监视点使能
  val datm = UInt(2.W) // 直接地址翻译模式时，load和store的存储访问类型
  val datf = UInt(2.W) // 直接地址翻译模式时，取值操作的存储访问类型
  val pg   = Bool()    // 映射地址翻译使能
  val da   = Bool()    // 直接地址翻译使能
  val ie   = Bool()    // 全局中断使能
  val plv  = UInt(2.W) // 特权等级
}

class CRMD extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new CRMD_info))
    init.da := true.B
    init
  })
  override val id = CSR.CRMD
}

class PRMD_info extends Bundle {
  val zero = UInt(28.W)
  val pwe  = Bool()    // 指令和数据监视点使能
  val pie  = Bool()    // 全局中断使能
  val pplv = UInt(2.W) // 特权等级
}

class PRMD extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new PRMD_info))
    init
  })
  override val id = CSR.PRMD
}

class ECFG_info extends Bundle {
  val zero1 = UInt(13.W)
  val vs    = UInt(3.W)
  val zero2 = UInt(3.W)
  val lie   = UInt(13.W)
}

class ECFG extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new ECFG_info))
    init
  })
  override val id = CSR.ECFG
}

class ESTAT_info extends Bundle {
  val zero1    = UInt(1.W)
  val esubcode = UInt(9.W)  // 例外类型二级编码
  val ecode    = UInt(6.W)  // 例外类型一级编码
  val zero2    = UInt(3.W)
  val is       = UInt(13.W) // 软件中断状态位

}

class ESTAT extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new ESTAT_info))
    init
  })
  override val id = CSR.ESTAT
}

class ERA_info extends Bundle with Parameters {
  val pc = UInt(ADDR_WIDTH.W)
}

class ERA extends base {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new ERA_info))
    init
  })
  override val id = CSR.ERA
}

class EENTRY_info extends Bundle with Parameters {
  val vpn  = UInt((ADDR_WIDTH - 12).W)
  val zero = UInt(12.W)
}

class EENTRY extends base with Parameters {
  override val info = RegInit({
    val init = WireDefault(0.U.asTypeOf(new EENTRY_info))
    init
  })
  override val id = CSR.EENTRY
}

class CSR_IO extends Bundle with Parameters {
  // from ds
  val re      = Input(Bool())
  val raddr   = Input(UInt(14.W))
  val rf_bus  = Input(new rf_bus)

  // from wb
  val info    = Input(new info)
  val start = Input(Bool())
  val end   = Input(Bool())

  // to ds
  val rdata   = Output(UInt(DATA_WIDTH.W))

  // to fs
  val exc_bus = Output(new exc_bus)
}

class CSR extends Module with Parameters {
  val io = IO(new CSR_IO)

  val CRMD   = new CRMD
  val PRMD   = new PRMD
  val ESTAT  = new ESTAT
  val ERA    = new ERA
  val EENTRY = new EENTRY
  val ECFG   = new ECFG

  // val EUEN      = RegInit(0.U(32.W))
  // val BADV      = RegInit(0.U(32.W))
  // val TLBIDX    = RegInit(0.U(32.W))
  // val TLBEHI    = RegInit(0.U(32.W))
  // val TLBELO0   = RegInit(0.U(32.W))
  // val TLBELO1   = RegInit(0.U(32.W))
  // val ASID      = RegInit(0.U(32.W))
  // val PGDL      = RegInit(0.U(32.W))
  // val PGDH      = RegInit(0.U(32.W))
  // val PGD       = RegInit(0.U(32.W))
  // val CPUID     = RegInit(0.U(32.W))
  // val SAVE0     = RegInit(0.U(32.W))
  // val SAVE1     = RegInit(0.U(32.W))
  // val SAVE2     = RegInit(0.U(32.W))
  // val SAVE3     = RegInit(0.U(32.W))
  // val TID       = RegInit(0.U(32.W))
  // val TCFG      = RegInit(0.U(32.W))
  // val TVAL      = RegInit(0.U(32.W))
  // val TICLR     = RegInit(0.U(32.W))
  // val LLBCTL    = RegInit(0.U(32.W))
  // val TLBRENTRY = RegInit(0.U(32.W))
  // val CTAG      = RegInit(0.U(32.W))
  // val DMW0      = RegInit(0.U(32.W))
  // val DMW1      = RegInit(0.U(32.W))

  val csrlist = Seq(
    CRMD,
    PRMD,
    // EUEN,
    ECFG,
    ESTAT,
    ERA,
    // BADV,
    EENTRY,
    // TLBIDX,
    // TLBEHI,
    // TLBELO0,
    // TLBELO1,
    // ASID,
    // PGDL,
    // PGDH,
    // PGD,
    // CPUID,
    // SAVE0,
    // SAVE1,
    // SAVE2,
    // SAVE3,
    // TID,
    // TCFG,
    // TVAL,
    // TICLR,
    // LLBCTL,
    // TLBRENTRY,
    // CTAG,
    // DMW0,
    // DMW1,
  )

  // 读 or 写
  io.rdata := 0.U
  when (io.re) {
    for (x <- csrlist) {
      when (io.raddr === x.id) {
        io.rdata := x.info.asUInt
      }
    }
  }

  when(io.rf_bus.we) {
    for (x <- csrlist) {
      when(io.rf_bus.waddr === x.id) {
        x.write(writeMask(io.rf_bus.wmask, x.info.asUInt, io.rf_bus.wdata))
      }
    }
  }

  // 不可写区域
  CRMD.info.zero   := 0.U
  PRMD.info.zero   := 0.U
  ESTAT.info.zero1 := 0.U
  ESTAT.info.zero2 := 0.U
  EENTRY.info.zero := 0.U
  ECFG.info.zero1  := 0.U
  ECFG.info.zero2  := 0.U

  // 例外跳转
  io.exc_bus := WireDefault(0.U.asTypeOf(new exc_bus))
  when(io.start) {
    PRMD.info.pplv := CRMD.info.plv
    PRMD.info.pie  := CRMD.info.ie
    CRMD.info.plv  := 0.U
    CRMD.info.ie   := 0.U
    ESTAT.info.ecode := MateDefault(
      io.info.op_type,
      0.U,
      List(
        ExcOpType.sys -> ECodes.SYS,
      ),
    )
    ERA.info.pc := io.info.pc

    io.exc_bus.en := true.B
    io.exc_bus.pc := EENTRY.info.asUInt
  }

  when(io.end) {
    CRMD.info.plv := PRMD.info.pplv
    CRMD.info.ie  := PRMD.info.pie

    io.exc_bus.en := true.B
    io.exc_bus.pc := ERA.info.pc
  }
}
