package pipeline.rob

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class RatTable extends Bundle {
  val map = Vec(1 << AREG_WIDTH, UInt(PREG_WIDTH.W))
}
class RatRecoverInfo extends Bundle {
}
class RatIO extends Bundle {
  
}

class Rat extends Module {
  val io = IO(new RatIO)
}