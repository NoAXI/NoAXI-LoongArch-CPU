package bundles

import chisel3._
import chisel3.util._

import const.Parameters._

class debug_wb extends Bundle {
  val pc       = Output(UInt(32.W))
  val rf_we    = Output(UInt(4.W))
  val rf_wnum  = Output(UInt(5.W))
  val rf_wdata = Output(UInt(32.W))
}
