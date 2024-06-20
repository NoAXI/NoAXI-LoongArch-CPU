package pipeline.frontend

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class RenameTopIO extends StageBundle {
}

class RenameTop extends Module {
  val io = IO(new RenameTopIO)
  
}