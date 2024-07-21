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
  val memory2  = Flipped(new Mem2BufferIO)
  val from     = Flipped(DecoupledIO(new BufferInfo)) // memory2
  val to       = DecoupledIO(new BufferInfo)
  val popValid = Input(Bool())
  val flush    = Input(Bool())
}

class StoreBuffer(
    entries: Int,
    // bufferType: String,
) extends Module {
  // require(Seq("st", "wb").contains(bufferType))

  val io = IO(new StoreBufferIO)

  // use compressive queue
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
        mem(i) := mem(i + 1)
      } else {
        mem(i) := 0.U.asTypeOf(mem(i))
      }
    }
  }

  // ptr update
  when(io.from.fire) {
    when(io.to.fire) {
      mem(topPtr - 1.U) := io.from.bits
    }.otherwise {
      mem(topPtr) := io.from.bits
    }
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
  when(io.flush) {
    topPtr    := 0.U
    maybeFull := false.B
    mem       := 0.U.asTypeOf(mem)
  }

  // handshake
  io.from.ready := !full || (io.from.valid && io.popValid)
  io.to.valid   := !empty && io.popValid
  io.to.bits    := mem(0)
}
