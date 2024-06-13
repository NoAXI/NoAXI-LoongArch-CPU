package Funcs

import chisel3._
import chisel3.util._

import bundles._

object Functions {
  // for pipelines---------------------------------------------------------------------------
  def StageConnect(x: DecoupledIO[info], y: DecoupledIO[info], busy: Bool): full_info = {
    val info  = RegInit(0.U.asTypeOf(new info))
    val valid = RegInit(false.B)
    x.ready := !valid || (y.ready && !busy)
    y.valid := valid && !busy
    when(x.ready) {
      valid := x.valid
    }
    when(x.fire) {
      info := x.bits
    }

    // return (info, valid_signal) as a bundle
    val res = Wire(new full_info)
    res.info         := info
    res.valid_signal := valid
    res
  }

  def Forward(x: info, y: ForwardData, valid_signal: Bool): Unit = {
    y.we   := x.iswf && valid_signal
    y.isld := x.isload
    y.addr := x.wfreg
    y.data := x.result

    y.csr_we   := x.csr_iswf && valid_signal
    y.csr_addr := x.csr_addr
    y.csr_data := x.rd

    y.pc := x.pc
  }

  def FlushWhen(x: info, y: Bool): Unit = {
    when(y) {
      x        := 0.U.asTypeOf(new info)
      x.bubble := true.B
    }
  }

  /*
      _________          _________
  ___|         |________|         |______

           _________          _________
  ________|         |________|         |______

       __                 __
  ____|  |_______________|  |____________

  can not be used in continuous sign
   */
  def FirstTick(x: Bool): Bool = {
    x && !ShiftRegister(x, 1)
  }

  // StallPrevious: just set busy to true.B

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
