package bundles

import chisel3._
import chisel3.util._

import const._
import const.Parameters._
import const.cacheConst._

class CsrReadIO extends Bundle {
  val addr = Input(UInt(CSR_WIDTH.W))
  val data = Output(UInt(DATA_WIDTH.W))
}

class ExcBasicInfo extends Bundle {
  val excType  = ECodes()
  val excVAddr = UInt(ADDR_WIDTH.W)
  val pc       = UInt(ADDR_WIDTH.W)
  val pc_add_4 = UInt(ADDR_WIDTH.W)
}

class ExcInfo extends Bundle {
  val en       = Bool()
  val excType  = ECodes()
  val excVAddr = UInt(ADDR_WIDTH.W)
}

class ExcHappenInfo extends Bundle {
  val start = Bool()
  val end   = Bool()
  val info  = new ExcBasicInfo
}
