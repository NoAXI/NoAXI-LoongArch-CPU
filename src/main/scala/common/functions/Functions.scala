package func

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

object Functions {
  // for pipelines---------------------------------------------------------------------------
  def stageConnect(x: DecoupledIO[DualInfo], y: DecoupledIO[DualInfo], busy: BusyInfo): (DualInfo, Bool) = {
    val info  = RegInit(0.U.asTypeOf(new DualInfo))
    val valid = RegInit(false.B)
    val stall = busy.info.reduce(_ || _)
    x.ready := !valid || (y.ready && !stall)
    y.valid := valid && !stall
    when(x.ready) {
      valid := x.valid
    }
    when(x.fire) {
      info := x.bits
    }
    (info, valid)
  }

  def flushWhen(infoReg: DualInfo, flush: Bool): Unit = {
    when(flush) {
      infoReg := 0.U.asTypeOf(new DualInfo)
      for (i <- 0 until ISSUE_WIDTH) {
        infoReg.bits(i).bubble := true.B
      }
    }
  }

  // for decoder--------------------------------------------------------------------------------
  def SignedExtend(a: UInt, len: Int) = {
    val aLen    = a.getWidth
    val signBit = a(aLen - 1)
    if (aLen >= len) a(len - 1, 0) else Cat(Fill(len - aLen, signBit), a)
  }

  def UnSignedExtend(a: UInt, len: Int) = {
    val aLen = a.getWidth
    if (aLen >= len) a(len - 1, 0) else Cat(0.U((len - aLen).W), a)
  }

  def MateDefault[T <: Data](key: UInt, default: T, map: Iterable[(UInt, T)]): T =
    MuxLookup(key, default)(map.toSeq)

  // for memory---------------------------------------------------------------------------------
  def Extend(a: UInt, len: Int, typ: Bool) = {
    Mux(typ, SignedExtend(a, len), UnSignedExtend(a, len))
  }

  // for csr------------------------------------------------------------------------------------
  def writeMask(mask: UInt, data: UInt, wdata: UInt): UInt = {
    (wdata & mask) | (data & ~mask)
  }

  // for cache----------------------------------------------------------------------------------
  def Merge(wstrb: UInt, linedata: UInt, wdata: UInt, offset: UInt): UInt = {
    val _wstrb = Cat((3 to 0 by -1).map(i => Fill(8, wstrb(i))))
    val _move  = VecInit(0.U, 32.U, 64.U, 96.U)
    writeMask(_wstrb << _move(offset), linedata, wdata << _move(offset))
  }
}
