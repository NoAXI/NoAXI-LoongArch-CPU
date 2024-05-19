package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import const.Parameters._

class csrRegRead extends Bundle {
  val re    = Output(Bool())
  val raddr = Output(UInt(CSR_WIDTH.W))
  val rdata = Input(UInt(DATA_WIDTH.W))
}
