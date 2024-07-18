package func

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._
import pipeline.AwakeInfo
import pipeline.ForwardInfoIO

object Functions {
  // for pipelines---------------------------------------------------------------------------
  // dual connect
  def stageConnect[T <: BasicStageInfo](x: DecoupledIO[T], y: DecoupledIO[T], stall: Bool, flush: Bool, hasRbStore: Boolean = false): (T, Bool) = {
    val info  = RegInit(0.U.asTypeOf(x.bits))
    val valid = RegInit(false.B)
    val wait  = RegInit(false.B)
    x.ready := !valid || (y.ready && !stall)
    y.valid := valid && !stall
    when(x.ready) {
      valid := x.valid
    }
    when(x.fire) {
      info := x.bits
    }
    // flush keeps just a moment
    val rbStore = WireDefault(false.B)
    if (hasRbStore) {
      val singleInfo = WireDefault(info).asTypeOf(new SingleInfo)
      rbStore := singleInfo.actualStore
    }
    when(!rbStore) {
      when(flush) {
        y.valid := false.B
        when(!stall) {
          info := info.getFlushInfo
        }.otherwise {
          wait := true.B
        }
      }
      when(wait && !stall) {
        wait := false.B
      }
      when(wait) {
        y.valid := false.B
      }
    }
    (info, valid)
  }
  // single connect
  // def stageConnect(x: DecoupledIO[SingleInfo], y: DecoupledIO[SingleInfo], busy: Bool, flush: Bool, rbStore: Bool = false.B): (SingleInfo, Bool) = {
  //   val info  = RegInit(0.U.asTypeOf(new SingleInfo))
  //   val valid = RegInit(false.B)
  //   val wait  = RegInit(false.B)
  //   val empty = WireDefault(false.B)
  //   val stall = busy
  //   x.ready := !valid || (y.ready && !stall)
  //   y.valid := valid && !stall
  //   when(x.ready) {
  //     valid := x.valid
  //   }
  //   when(x.fire) {
  //     info := x.bits
  //   }
  //   // flush keeps just a moment
  //   when(flush) {
  //     when(!stall) {
  //       info := 0.U.asTypeOf(new SingleInfo)
  //     }.otherwise {
  //       wait := true.B
  //     }
  //   }
  //   when(wait && !stall) {
  //     empty := true.B
  //     wait  := false.B
  //   }
  //   (Mux(empty, 0.U.asTypeOf(new SingleInfo), info), valid)
  // }
  // preFetch pc
  def nextPC(pc: UInt): UInt = {
    Mux(~pc(2), pc(ADDR_WIDTH - 1, 2) + 2.U, pc(ADDR_WIDTH - 1, 2) + 1.U) ## 0.U(2.W)
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

  // for issue queue ---------------------------------------------------------------------------
  def checkIssueHit(rj: UInt, rk: UInt, awake: Vec[AwakeInfo], busy: Vec[Bool]): Vec[Bool] = {
    val awakeHit = WireDefault(VecInit(Seq.fill(OPERAND_MAX)(false.B)))
    for (i <- 0 until AWAKE_NUM) {
      for (j <- 0 until OPERAND_MAX) {
        val preg = if (j == 0) rj else rk
        when(awake(i).valid && awake(i).preg === preg) {
          awakeHit(j) := true.B
        }
      }
    }
    val realHit = VecInit(Seq.fill(OPERAND_MAX)(false.B))
    for (j <- 0 until OPERAND_MAX) {
      val preg = if (j == 0) rj else rk
      realHit(j) := awakeHit(j) || !busy(preg)
    }
    if (Config.debug_on) {
      dontTouch(awakeHit)
      dontTouch(realHit)
    }
    realHit
  }

  // for forwarder ---------------------------------------------------------------------------
  def doForward(io: ForwardInfoIO, info: SingleInfo, validInst: Bool): Unit = {
    io.valid := validInst && info.iswf && info.rdInfo.areg =/= 0.U
    io.data  := info.rdInfo.data
    io.preg  := info.rdInfo.preg
  }

  // for compressing queue ---------------------------------------------------------------------------
  def doCompress[T <: Data](hitVec: Vec[Bool], entries: Int, mem: Vec[T]): Unit = {
    val shiftVec = WireDefault(VecInit(Seq.fill(entries)(false.B)))
    for (i <- 0 until entries) {
      if (i > 0) {
        shiftVec(i) := shiftVec(i - 1) | hitVec(i)
      } else {
        shiftVec(i) := hitVec(i)
      }
    }
    for (i <- 0 until entries) {
      when(shiftVec(i)) {
        if (i < (entries - 1)) {
          mem(i) := mem(i + 1)
        } else {
          mem(i) := 0.U.asTypeOf(mem(i))
        }
      }
    }
  }
  def doCompressPtrMove[memType <: Data](
      from: DecoupledIO[Data],
      topPtr: UInt,
      incSignal: Bool,
      decSignal: Bool,
      maybeFull: Bool,
      mem: Vec[memType],
  ) = {
    when(from.fire) {
      mem(topPtr) := from.bits
    }
    when(incSignal =/= decSignal) {
      when(incSignal) {
        topPtr    := topPtr + 1.U
        maybeFull := true.B
      }.otherwise {
        topPtr    := topPtr - 1.U
        maybeFull := false.B
      }
    }
  }
}
