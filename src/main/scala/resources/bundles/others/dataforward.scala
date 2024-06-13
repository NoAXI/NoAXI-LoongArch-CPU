package bundles

import chisel3._
import chisel3.util._

import const.Parameters._

class ForwardData extends Bundle {
  val pc   = UInt(ADDR_WIDTH.W)
  val we   = Bool()
  val isld = Bool()
  val addr = UInt(REG_WIDTH.W)
  val data = UInt(DATA_WIDTH.W)

  val csr_we   = Bool()
  val csr_addr = UInt(CSR_WIDTH.W)
  val csr_data = UInt(DATA_WIDTH.W)
}

class ForwardQuery extends Bundle {
  val addr     = Vec(3, UInt(REG_WIDTH.W))
  val ini_data = Vec(3, UInt(DATA_WIDTH.W))

  val csr_addr     = UInt(CSR_WIDTH.W)
  val csr_ini_data = UInt(DATA_WIDTH.W)
}

class ForwardAns extends Bundle {
  val notld = Bool()
  val data  = Vec(3, UInt(DATA_WIDTH.W))

  val csr_data = UInt(DATA_WIDTH.W)
}
