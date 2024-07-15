package isa

import chisel3._
import chisel3.util._

import const.Parameters._

object FuncType {
  def none    = "b0000".U
  def bru     = "b0001".U
  def alu     = "b0010".U
  def mem     = "b0011".U
  def div     = "b0100".U
  def mul     = "b0101".U
  def alu_imm = "b0110".U
  def csr     = "b0111".U
  def exc     = "b1000".U
  def tlb     = "b1001".U
  def cnt     = "b1010".U
  def apply() = UInt(FUNC_TYPE_WIDTH.W)
}
