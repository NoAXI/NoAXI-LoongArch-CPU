package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class CommitTopIO extends Bundle {
  
}

class CommitTop extends Module {
  val io = IO(new CommitTopIO)
  
}