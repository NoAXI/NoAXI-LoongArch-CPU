package pipeline.backend

import chisel3._
import chisel3.util._

import const._
import bundles._
import Funcs.Functions._
import const.Parameters._

class FreelistRobInfo extends Bundle {
  val valid = Input(Bool())
  val preg  = Input(UInt(PREG_WIDTH.W))
}
class FreelistQuery extends Bundle {
  val valid = Input(Bool())
  val preg  = Output(UInt(PREG_WIDTH.W))
}
class FreelistIO extends Bundle {
  val rob = Vec(2, new FreelistRobInfo)
  val res = Vec(2, new FreelistQuery)
}
class Freelist extends Module {
  val io = IO(new FreelistIO)
}
