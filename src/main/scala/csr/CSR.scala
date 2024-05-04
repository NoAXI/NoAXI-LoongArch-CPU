package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class CRMD extends Bundle {
  val zero = UInt(22.W)
  val we   = Bool()    // 指令和数据监视点使能
  val datm = UInt(2.W) // 直接地址翻译模式时，load和store的存储访问类型
  val datf = UInt(2.W) // 直接地址翻译模式时，取值操作的存储访问类型
  val pg   = Bool()    // 映射地址翻译使能
  val da   = Bool()    // 直接地址翻译使能
  val ie   = Bool()    // 全局中断使能
  val plv  = UInt(2.W) // 特权等级
}

class PRMD extends Bundle {
  val zero = UInt(28.W)
  val pwe  = Bool()
  val pie  = Bool()
  val pplv = UInt(2.W)
}

class ECFG extends Bundle {
  val zero1 = UInt(13.W)
  val vs    = UInt(3.W)
  val zero2 = UInt(3.W)
  val lie   = UInt(13.W)
}

class ESTAT extends Bundle {
  val zero1    = UInt(1.W)
  val esubcode = UInt(9.W)  // 例外类型二级编码
  val ecode    = UInt(6.W)  // 例外类型一级编码
  val zero2    = UInt(3.W)
  val is       = UInt(13.W) // 软件中断状态位
}

class ERA extends Bundle with Parameters {
  val pc = UInt(ADDR_WIDTH.W)
}

class EENTRY extends Bundle with Parameters {
  val vpn  = UInt((ADDR_WIDTH - 12).W)
  val zero = UInt(12.W)
}

object f {
  def enter(csr: CSR, info: info): Unit = {
    csr.PRMD.pplv := csr.CRMD.plv
    csr.PRMD.pie  := csr.CRMD.ie
    csr.CRMD.plv  := 0.U
    csr.CRMD.ie   := 0.U
    csr.ERA       := info.pc

    // csr.io.exc_bus.en     := true.B
    // csr.io.exc_bus.exc_pc := entryPC(csr)
  }

  def back(csr: CSR): Unit = {
    csr.CRMD.plv := csr.PRMD.pplv
    csr.CRMD.ie  := csr.PRMD.pie

    // csr.io.exc_bus.en     := true.B
    // csr.io.exc_bus.exc_pc := csr.ERA
  }

  def entryPC(csr: CSR): UInt = {
    // csr.EENTRY | Mux(csr.ECFG.vs === 0.U, xxx, info.ecode << (x.reg(CSR.ECFG).vs + 2.U))
    csr.EENTRY.asUInt | ECodes.SYS << (csr.ECFG.vs + 2.U)
  }
}

class CSR_IO extends Bundle with Parameters {
  val re     = Input(Bool())
  val raddr  = Input(UInt(14.W))
  val rf_bus = Input(new rf_bus)
  val info   = Input(new info)
  val rdata  = Output(UInt(32.W))
}

class CSR extends Module with Parameters {
  val io = IO(new CSR_IO)

  val CRMD = RegInit(0.U.asTypeOf(new CRMD))
  CRMD.da := RegInit(true.B) // 真的，你让我de了很久，众里寻他千百度，蓦然回首，却藏在指令手册深处
  val PRMD   = RegInit(0.U.asTypeOf(new PRMD))
  val ESTAT  = RegInit(0.U.asTypeOf(new ESTAT))
  val ERA    = RegInit(0.U.asTypeOf(new ERA))
  val EENTRY = RegInit(0.U.asTypeOf(new EENTRY))
  val ECFG   = RegInit(0.U.asTypeOf(new ECFG))

  io.rdata := Mux(
    io.re,
    MateDefault(
      io.raddr,
      0.U,
      List(
        CSR.CRMD   -> CRMD.asUInt,
        CSR.PRMD   -> PRMD.asUInt,
        CSR.ESTAT  -> ESTAT.asUInt,
        CSR.ERA    -> ERA.asUInt,
        CSR.EENTRY -> EENTRY.asUInt,
        CSR.ECFG   -> ECFG.asUInt,
      ),
    ),
    0.U,
  )

  when(io.rf_bus.we) {
    switch(io.rf_bus.waddr) {
      is(CSR.CRMD) {
        // val wdata = writeMask(io.rf_bus.wmask, CRMD.asUInt, io.rf_bus.wdata)
        CRMD := io.rf_bus.wdata.asTypeOf(new CRMD)
      }
      is(CSR.PRMD) {
        // PRMD := writeMask(io.rf_bus.wmask, PRMD.asUInt, io.rf_bus.wdata).asTypeOf(new PRMD)
        PRMD := io.rf_bus.wdata.asTypeOf(new PRMD)
      }
      is(CSR.ESTAT) {
        // ESTAT := writeMask(io.rf_bus.wmask, ESTAT.asUInt, io.rf_bus.wdata).asTypeOf(new ESTAT)
        ESTAT := io.rf_bus.wdata.asTypeOf(new ESTAT)
      }
      is(CSR.ERA) {
        // ERA := writeMask(io.rf_bus.wmask, ERA.asUInt, io.rf_bus.wdata).asTypeOf(new ERA)
        ERA := io.rf_bus.wdata.asTypeOf(new ERA)
      }
      is(CSR.EENTRY) {
        // EENTRY := writeMask(io.rf_bus.wmask, EENTRY.asUInt, io.rf_bus.wdata).asTypeOf(new EENTRY)
        EENTRY := io.rf_bus.wdata.asTypeOf(new EENTRY)
      }
      is(CSR.ECFG) {
        // ECFG := writeMask(io.rf_bus.wmask, ECFG.asUInt, io.rf_bus.wdata).asTypeOf(new ECFG)
        ECFG := io.rf_bus.wdata.asTypeOf(new ECFG)
      }
    }
  }

  CRMD.zero   := 0.U
  PRMD.zero   := 0.U
  ESTAT.zero1 := 0.U
  ESTAT.zero2 := 0.U
  EENTRY.zero := 0.U
  ECFG.zero1  := 0.U
  ECFG.zero2  := 0.U
}
