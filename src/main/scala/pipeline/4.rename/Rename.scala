package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class RenameIO extends Bundle {
  // val info = Vec(ISSUE_WIDTH, new RenameBundleIO)
}

class Rename extends Module {
  val io = IO(new RenameIO)
  val query_bit = Vec(ISSUE_WIDTH, Bool())
  for(i <- 0 until ISSUE_WIDTH) {
    // val info = io.info(i)
    // when(info.valid) {
      
    // }
  }
}
