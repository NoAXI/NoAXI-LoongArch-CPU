package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class BufferInfo extends Bundle {
  val valid = Bool()
  val addr  = UInt(ADDR_WIDTH.W) // paddr
  val data  = UInt(DATA_WIDTH.W)
}

class StoreBufferIO extends Bundle {
  val mem   = Flipped(new MemBufferIO)
  val from  = Flipped(DecoupledIO(new BufferInfo))
  val to    = DecoupledIO(new BufferInfo)
  val flush = Input(Bool())
}

class StoreBuffer(
    entries: Int,
    bufferType: String,
) extends Module {
  assert(Seq("st", "wb").contains(bufferType))

  val io = IO(new StoreBufferIO)

  // use compressive queue
  val buffer = Seq.fill(entries)(new BufferInfo)
  val hit    = WireDefault(false.B)
  val data   = WireDefault(UInt(DATA_WIDTH.W))
  for (i <- 0 until entries) {
    when(io.mem.pa === buffer(i).addr && buffer(i).valid) {
      hit         := true.B
      io.mem.data := buffer(i).data
    }
  }

  val mem       = RegInit(VecInit(Seq.fill(entries)(0.U.asTypeOf(new SingleInfo))))
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
    mem(topPtr) := io.from.bits
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
  }

  // handshake
  io.from.ready := !full
  io.to.valid   := !empty
  io.to.bits    := mem(0)
}
