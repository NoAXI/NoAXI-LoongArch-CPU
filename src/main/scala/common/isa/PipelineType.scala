package isa

import chisel3._
import chisel3.util._

import const.Parameters._

object PipelineType {
  def nop     = 0.U
  def arith   = 1.U // real index: 0, 1
  def muldiv  = 2.U // real index: 2
  def memory  = 3.U // real index: 3
  def apply() = UInt(PIPELINE_TYPE_WIDTH.W)
}
