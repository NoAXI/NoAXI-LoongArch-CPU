package controller

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

object StallType {
  def idle    = "00".U
  def cacop   = "01".U
  def ll      = "10".U
  def apply() = UInt(2.W)

  def typeCount: Int = 3
}

class StallCtrlIO extends Bundle {
  val stallInfo = Input(new BranchInfo) // from rob
  val stallType = Input(StallType())

  val idleSignal  = Input(Bool())
  val cacopSignal = Input(Bool())
  val llSignal    = Input(Bool())

  val frontStall   = Output(Bool())
  val stallRecover = Output(new BranchInfo)
}

class StallCtrl extends Module {
  val io       = IO(new StallCtrlIO)
  val stallReg = RegInit(0.U.asTypeOf(io.stallInfo))
  val typeReg  = RegInit(0.U.asTypeOf(UInt(StallType.typeCount.W)))
  val signal   = Cat(io.llSignal, io.cacopSignal, io.idleSignal) // when you update optype, update this signal as well

  when(io.stallInfo.en) {
    stallReg := io.stallInfo
    typeReg  := (UIntToOH(io.stallType))(StallType.typeCount - 1, 0)
  }

  when(stallReg.en && (signal & typeReg) =/= 0.U) {
    stallReg        := 0.U.asTypeOf(stallReg)
    typeReg         := 0.U.asTypeOf(typeReg)
    io.stallRecover := stallReg
  }.otherwise {
    io.stallRecover := 0.U.asTypeOf(io.stallRecover)
  }

  io.frontStall := stallReg.en
}
