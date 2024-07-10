package utils

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class MultiPortFIFOVec[T <: Data](
    dataType: T,
    depth: Int,
    pushPorts: Int,
    popPorts: Int,
    dataInit: Int => T = null,
    pushInit: Int = 0,
    popInit: Int = 0,
    initFull: Boolean = false,
) extends Module {
  require(depth > 0 && isPow2(depth))
  val addressWidth = log2Ceil(depth)
  val dataVec = if (dataInit != null) {
    VecInit(Seq.tabulate(depth)(i => RegInit(dataInit(i))))
  } else {
    Reg(Vec(depth, dataType))
  }

  def dataRead(addr: UInt)            = dataVec(addr)
  def dataWrite(addr: UInt, value: T) = dataVec(addr) := value

  val pushPtr    = RegInit(UInt(addressWidth.W), pushInit.U)
  val popPtr     = RegInit(UInt(addressWidth.W), popInit.U)
  val pushEquals = VecInit((0 until depth).map(i => pushPtr === i.U))
  val popEquals  = VecInit((0 until depth).map(i => popPtr === i.U))

  def dataRead(base: UInt, offset: Int) = {
    val result = Wire(dataType)
    result := DontCare
    for ((d, i) <- dataVec.zipWithIndex) {
      when(popEquals((i - offset + depth) % depth)) {
        result := d
      }
    }
    result
  }

  def dataWrite(base: UInt, offset: Int, value: T) = {
    for ((d, i) <- dataVec.zipWithIndex) {
      when(pushEquals((i - offset + depth) % depth)) {
        d := value
      }
    }
  }

  val isRisingOccupancy = RegInit(initFull.B) // 是否正在增长
  val isEmpty           = pushPtr === popPtr && !isRisingOccupancy
  val isFull            = pushPtr === popPtr && isRisingOccupancy

  val io = IO(new Bundle {
    val push = Vec(pushPorts, Flipped(Decoupled(dataType)))
    val pop  = Vec(popPorts, Decoupled(dataType))
  })

  val maxPush = popPtr - pushPtr
  val pushCount = PriorityMux(io.push.zipWithIndex.map { case (p, i) =>
    !p.fire -> i.U
  } :+ (true.B -> pushPorts.U))

  for (i <- 0 until pushPorts) {
    val validTakeLeft = io.push.take(i + 1).map(_.valid).reduce(_ && _)
    io.push(i).ready := isEmpty || i.U < maxPush
    when(io.push(i).ready && validTakeLeft) {
      dataWrite(pushPtr, i, io.push(i).bits)
    }
  }
  pushPtr := pushPtr + pushCount

  val maxPop = pushPtr - popPtr
  val popCount = PriorityMux(io.pop.zipWithIndex.map { case (p, i) =>
    !p.fire -> i.U
  } :+ (true.B -> popPorts.U))

  for (i <- 0 until popPorts) {
    val readyTakeLeft = io.pop.take(i + 1).map(_.ready).reduce(_ && _)
    io.pop(i).valid := isFull || i.U < maxPop
    io.pop(i).bits  := dataRead(popPtr, i)
  }
  popPtr := popPtr + popCount

  when(pushCount =/= popCount) {
    isRisingOccupancy := pushCount > popCount
  }
}
