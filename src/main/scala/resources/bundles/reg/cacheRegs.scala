package bundles

import chisel3._
import chisel3.util._

import const.cacheConst._
import const.Parameters._

class Line extends Bundle {
  val valid = Bool()
  val addr  = UInt(ADDR_WIDTH.W)
  val data  = UInt(128.W)
}

class savedInfo extends Bundle {
  val linedata = Vec(2, UInt(128.W))
  val linetag  = Vec(2, UInt(TAG_WIDTH.W))
  val wstrb    = UInt(4.W)
  val wdata    = UInt(DATA_WIDTH.W)
  val addr     = UInt(ADDR_WIDTH.W)
  val op       = Bool() // 0: read    1: write
  def index    = addr(11, 4)
  def tag      = addr(31, 12)
  def offset   = addr(3, 2)
}
