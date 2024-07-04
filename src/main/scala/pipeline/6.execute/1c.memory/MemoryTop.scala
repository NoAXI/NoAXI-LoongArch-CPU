package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

// this pipeline contains of following inst
// memory access inst
// branch check inst
class MemoryTopIO extends Bundle {
}
class MemoryTop extends Module {
  val io = IO(new MemoryTopIO)
  
}