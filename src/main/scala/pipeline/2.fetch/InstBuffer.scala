package pipeline

import chisel3._
import chisel3.util._
import const.Parameters._
import bundles._
import func.Functions._
import const.ECodes

// to dec：pc、inst、exc(dont need)

object InstBufferConst {
  val IB_LENGTH = 2 * FETCH_DEPTH     // 缓存大小
  val IB_WIDTH  = log2Ceil(IB_LENGTH) // 地址宽度
  val W_LENGTH  = FETCH_DEPTH         // 取指深度
  val R_LENGTH  = ISSUE_WIDTH         // 读取深度
}

import InstBufferConst._

class InstBufferIO extends StageBundle {}

class InstBuffer extends Module {
  val io   = IO(new InstBufferIO)
  val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
  val from = stageConnect(io.from, io.to, busy)
  val info = from._1.bits(0)

  val instBuffer = RegInit(VecInit(Seq.fill(IB_LENGTH)(0.U(INST_WIDTH.W))))
  val pcBuffer   = RegInit(VecInit(Seq.fill(IB_LENGTH)(0.U(ADDR_WIDTH.W))))
  val excBuffer  = RegInit(VecInit(Seq.fill(IB_LENGTH)(0.U(7.W))))

  // [head, tail)
  val headPtr  = RegInit(0.U(IB_WIDTH.W))
  val tailPtr  = RegInit(0.U(IB_WIDTH.W))
  val fifoSize = RegInit(0.U((IB_WIDTH + 1).W))
  val freeSize = IB_LENGTH.U - fifoSize

  def push(i: Int): Unit = {
    instBuffer(tailPtr) := info.instV(i).inst
    pcBuffer(tailPtr)   := info.pc(31, 4) ## i.U(2.W) ## 0.U(2.W)
    excBuffer(tailPtr)  := info.fetchExc(i)
    tailPtr             := tailPtr + 1.U
    fifoSize            := fifoSize + 1.U
  }

  def pop(): (UInt, UInt, UInt) = {
    headPtr  := headPtr + 1.U
    fifoSize := fifoSize - 1.U
    (pcBuffer(headPtr), instBuffer(headPtr), excBuffer(headPtr))
  }

  // is this too complex?
  val canInsert = freeSize >= PopCount(VecInit(info.instV.map(_.valid)))
  val canRead   = fifoSize >= 2.U

  when(canInsert) {
    for (i <- 0 until W_LENGTH) {
      when(info.pc(3, 2) >= i.U && info.instV(i).valid) {
        push(i)
        info.instV(i).valid := false.B
      }
    }
  }.otherwise {
    busy.info(0) := true.B
  }

  io.to.bits := 0.U.asTypeOf(new DualInfo)
  when(canRead && io.to.ready) {
    for (i <- 0 until R_LENGTH) {
      val popResult = pop()
      io.to.bits.bits(i).pc       := popResult._1
      io.to.bits.bits(i).inst     := popResult._2
      io.to.bits.bits(i).exc_type := popResult._3
      // TODO: badvaddr
      io.to.bits.bits(i).predict := info.predict
    }
  }

  when(io.flush) {
    headPtr  := 0.U
    tailPtr  := 0.U
    fifoSize := 0.U
  }
}
