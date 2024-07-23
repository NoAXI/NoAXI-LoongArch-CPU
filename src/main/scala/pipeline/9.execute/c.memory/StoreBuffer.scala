package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class BufferInfo extends Bundle {
  val valid       = Bool()
  val requestInfo = new RequestInfo
}

class StoreBufferIO extends Bundle {
  val memory2       = Flipped(new Mem2BufferIO)
  val from          = Flipped(DecoupledIO(new BufferInfo)) // memory2
  val to            = DecoupledIO(new BufferInfo)
  val popValid      = Input(Bool())
  val flush         = Input(Bool())
  val committedBusy = Output(new BusyRegUpdateInfo)
}

class StoreBuffer(
    entries: Int,
    // bufferType: String,
) extends Module {
  // require(Seq("st", "wb").contains(bufferType))

  val io = IO(new StoreBufferIO)

  // use compressive queue
  val validReg = RegInit(VecInit(Seq.fill(entries)(false.B)))
  val validTop = RegInit(0.U(log2Ceil(entries).W))

  val mem = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(new BufferInfo))))
  val hit = WireDefault(false.B)

  io.memory2.forwardData := WireDefault(0.U.asTypeOf(io.memory2.forwardData))
  io.memory2.forwardStrb := WireDefault(0.U.asTypeOf(io.memory2.forwardStrb))

  // for (i <- 0 until entries) {
  //   when(io.memory1.forwardpa === mem(i).requestInfo.addr && mem(i).valid) {
  //     hit                    := true.B
  //     io.memory1.forwardData := mem(i).requestInfo.wdata
  //     io.memory1.forwardStrb := mem(i).requestInfo.wstrb
  //   }
  // }

  val bitHit  = WireDefault(VecInit(Seq.fill(4)(0.U(8.W))))
  val bitStrb = WireDefault(VecInit(Seq.fill(4)(false.B)))
  for (i <- 0 until entries) {
    when(io.memory2.forwardpa === mem(i).requestInfo.addr && mem(i).valid && mem(i).requestInfo.rbType) {
      hit := true.B
      for (j <- 0 to 3) {
        when(mem(i).requestInfo.wstrb(j)) {
          hit        := true.B
          bitStrb(j) := true.B
          bitHit(j)  := mem(i).requestInfo.wdata(j * 8 + 7, j * 8)
        }
      }
    }
  }
  io.memory2.forwardHit  := hit
  io.memory2.forwardData := bitHit.asUInt
  io.memory2.forwardStrb := bitStrb.asUInt

  val topPtr    = RegInit(0.U(log2Ceil(entries).W))
  val maybeFull = RegInit(false.B)
  val full      = maybeFull && topPtr === 0.U
  val empty     = !maybeFull && topPtr === 0.U

  // info shifting
  for (i <- 0 until entries) {
    when(io.to.fire) {
      if (i < (entries - 1)) {
        mem(i)      := mem(i + 1)
        validReg(i) := validReg(i + 1)
      } else {
        mem(i)      := 0.U.asTypeOf(mem(i))
        validReg(i) := false.B
      }
    }
  }

  // ptr update
  when(io.from.fire) {
    val pushPos = Mux(io.to.fire, topPtr - 1.U, topPtr)
    mem(pushPos)      := io.from.bits
    validReg(pushPos) := false.B
  }
  when(io.from.fire =/= io.to.fire) {
    when(io.from.fire) {
      topPtr    := topPtr + 1.U
      maybeFull := true.B
    }.otherwise {
      topPtr    := topPtr - 1.U
      maybeFull := false.B
    }
  }

  // committed valid reg update
  io.committedBusy := 0.U.asTypeOf(io.committedBusy)
  val validPos       = Mux(io.to.fire, validTop - 1.U, validTop)
  val validPosReg    = RegNext(validPos)
  val validHappenReg = RegNext(io.popValid)
  when(io.popValid) {
    validReg(validPos) := true.B
  }
  when(validHappenReg) {
    io.committedBusy.preg  := mem(validPosReg).requestInfo.wdata(PREG_WIDTH - 1, 0)
    io.committedBusy.valid := !mem(validPosReg).requestInfo.rbType && io.committedBusy.preg =/= 0.U
  }
  when(io.popValid =/= io.to.fire) {
    when(io.popValid) {
      validTop := validTop + 1.U
    }.otherwise {
      validTop := validTop - 1.U
    }
  }

  when(io.flush) {
    topPtr    := validTop
    maybeFull := false.B
    for (i <- 0 until entries) {
      when(!validReg(i)) {
        mem(i) := 0.U.asTypeOf(mem(i))
      }
    }
  }
  // handshake
  io.from.ready := !full || (io.from.valid && io.to.valid)
  io.to.valid   := !empty && validReg(0) && !io.flush
  io.to.bits    := mem(0)
}
