package pipeline

import chisel3._
import chisel3.util._
import const.Parameters._
import bundles._
import func.Functions._
import const.ECodes
import utils._

// to dec：pc、inst、exc(dont need)

object InstBufferConst {
  val IB_LENGTH = 8                   // 缓存大小
  val IB_WIDTH  = log2Ceil(IB_LENGTH) // 地址宽度
  val W_LENGTH  = FETCH_DEPTH         // 取指深度
  val R_LENGTH  = ISSUE_WIDTH         // 读取深度
}

import InstBufferConst._

class InstBufferInfo extends Bundle {
  val pc      = UInt(ADDR_WIDTH.W)
  val inst    = UInt(ADDR_WIDTH.W)
  val excType = ECodes()
  val predict = new BranchInfo
}

class InstBufferIO extends StageBundle {
  val stall = Input(Bool()) // ctrl after
}

class InstBuffer extends Module {
  val io = IO(new InstBufferIO)

  io.to.bits := 0.U.asTypeOf(new DualInfo)

  val fifo = Module(new MultiPortFifo(IB_LENGTH, new InstBufferInfo, forIB = true)).io
  fifo.flush := io.flush

  val pcGroup      = VecInit(io.from.bits.bits(0).pc, io.from.bits.bits(0).pc_add_4)
  val instGroup    = VecInit(io.from.bits.bits(0).instGroup(0), io.from.bits.bits(0).instGroup(1))
  val excGroup     = VecInit(io.from.bits.bits(0).fetchExc(0), io.from.bits.bits(0).fetchExc(1))
  val predictGroup = VecInit(io.from.bits.bits(0).predict, io.from.bits.bits(0).predict)

  for (i <- 0 until FETCH_DEPTH) {
    fifo.push(i).valid        := io.from.valid && io.from.bits.bits(0).instGroupValid(i)
    fifo.push(i).bits.pc      := pcGroup(i)
    fifo.push(i).bits.inst    := instGroup(i)
    fifo.push(i).bits.excType := excGroup(i)
    fifo.push(i).bits.predict := predictGroup(i)
  }
  io.from.ready := fifo.push(0).ready && fifo.push(1).ready

  for (i <- 0 until ISSUE_WIDTH) {
    fifo.pop(i).ready            := io.to.ready
    io.to.bits.bits(i).pc        := fifo.pop(i).bits.pc
    io.to.bits.bits(i).inst      := fifo.pop(i).bits.inst
    io.to.bits.bits(i).exc_type  := fifo.pop(i).bits.excType
    io.to.bits.bits(i).predict   := fifo.pop(i).bits.predict
    io.to.bits.bits(i).exc_vaddr := fifo.pop(i).bits.pc // 一定是pc
  }
  io.to.valid := fifo.pop(0).valid && fifo.pop(1).valid && !io.stall
}
// class InstBuffer extends Module {
//   val io   = IO(new InstBufferIO)
//   val busy = WireDefault(0.U.asTypeOf(new BusyInfo))
//   val from = stageConnect(io.from, io.to, busy)

//   val info = WireDefault(from._1.bits(0))
//   val res  = WireDefault(from._1)

//   val instBuffer = RegInit(VecInit(Seq.fill(IB_LENGTH)(0.U(INST_WIDTH.W))))
//   val pcBuffer   = RegInit(VecInit(Seq.fill(IB_LENGTH)(0.U(ADDR_WIDTH.W))))
//   val excBuffer  = RegInit(VecInit(Seq.fill(IB_LENGTH)(0.U(7.W))))

//   // [head, tail)
//   val headPtr    = RegInit(0.U(IB_WIDTH.W))
//   val head_add_1 = headPtr + 1.U
//   val head_add_2 = headPtr + 2.U
//   val tailPtr    = RegInit(0.U(IB_WIDTH.W))
//   val tail_add_1 = tailPtr + 1.U
//   val tail_add_2 = tailPtr + 2.U
//   val fifoSize   = RegInit(0.U((IB_WIDTH + 1).W))
//   val freeSize   = IB_LENGTH.U - fifoSize

//   // def push(i: Int): Unit = {
//   //   instBuffer(tailPtr) := info.instGroup(i)
//   //   pcBuffer(tailPtr)   := info.pc(31, 4) ## i.U(2.W) ## 0.U(2.W)
//   //   excBuffer(tailPtr)  := info.fetchExc(i)
//   //   tailPtr             := tailPtr + 1.U
//   //   fifoSize            := fifoSize + 1.U
//   // }

//   // def pop(): (UInt, UInt, UInt) = {
//   //   headPtr  := headPtr + 1.U
//   //   fifoSize := fifoSize - 1.U
//   //   (pcBuffer(headPtr), instBuffer(headPtr), excBuffer(headPtr))
//   // }

//   // is this too complex?
//   val canInsert = freeSize >= PopCount(info.instGroupValid)
//   val canRead   = fifoSize >= 2.U

//   when(canInsert) {
//     switch(info.instGroupValid.asUInt) {
//       is("b00".U) {}
//       is("b01".U) {
//         instBuffer(tailPtr) := info.instGroup(0)
//         pcBuffer(tailPtr)   := info.pc
//         excBuffer(tailPtr)  := info.fetchExc(0)
//         tailPtr             := tail_add_1
//         fifoSize            := fifoSize + 1.U
//       }
//       is("b10".U) {
//         instBuffer(tailPtr) := info.instGroup(1)
//         pcBuffer(tailPtr)   := info.pc_add_4
//         excBuffer(tailPtr)  := info.fetchExc(1)
//         tailPtr             := tail_add_1
//         fifoSize            := fifoSize + 1.U
//       }
//       is("b11".U) {
//         instBuffer(tailPtr)    := info.instGroup(0)
//         pcBuffer(tailPtr)      := info.pc
//         excBuffer(tailPtr)     := info.fetchExc(0)
//         instBuffer(tail_add_1) := info.instGroup(1)
//         pcBuffer(tail_add_1)   := info.pc_add_4
//         excBuffer(tail_add_1)  := info.fetchExc(1)
//         tailPtr                := tail_add_2
//         fifoSize               := fifoSize + 2.U
//       }
//     }
//   }

//   when(io.flush) {
//     headPtr  := 0.U
//     tailPtr  := 0.U
//     fifoSize := 0.U
//   }

//   when(!canRead) {
//     io.to.valid := false.B
//   }

//   when(canRead && io.to.ready && !io.stall) {
//     res.bits(0).pc       := pcBuffer(headPtr)
//     res.bits(0).inst     := instBuffer(headPtr)
//     res.bits(0).exc_type := excBuffer(headPtr)
//     res.bits(1).pc       := pcBuffer(head_add_1)
//     res.bits(1).inst     := instBuffer(head_add_1)
//     res.bits(1).exc_type := excBuffer(head_add_1)
//     headPtr              := head_add_2
//     fifoSize             := fifoSize - 2.U
//     // TODO: badvaddr

//     res.bits(0).predict := info.predict
//     res.bits(1).predict := info.predict
//   }

//   io.to.bits := res
// }
