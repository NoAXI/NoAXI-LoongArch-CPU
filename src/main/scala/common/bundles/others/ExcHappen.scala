package bundles

import chisel3._
import chisel3.util._

import isa._
import const._
import const.Parameters._

class excHappen extends Bundle {
  val start = Bool()
  val end   = Bool()
  val info  = new SingleInfo
}
