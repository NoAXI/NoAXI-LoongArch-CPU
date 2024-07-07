package bundles

import chisel3._
import chisel3.util._

import const.Parameters._

class AR extends Bundle {
  val id    = UInt(4.W)  // transaction ID
  val addr  = UInt(32.W) // address
  val len   = UInt(8.W)  // burst length
  val size  = UInt(3.W)  // transfer size
  val burst = UInt(2.W)  // burst type
  val lock  = UInt(2.W)  // lock type
  val cache = UInt(4.W)  // cache type
  val prot  = UInt(3.W)  // protection type
}

class R extends Bundle {
  val id   = UInt(4.W)  // transaction ID
  val data = UInt(32.W) // read data
  val resp = UInt(2.W)  // response type
  val last = Bool()     // last beat of burst
}

class AW extends Bundle {
  val id    = UInt(4.W)  // transaction ID
  val addr  = UInt(32.W) // address
  val len   = UInt(8.W)  // burst length
  val size  = UInt(3.W)  // transfer size
  val burst = UInt(2.W)  // burst type
  val lock  = UInt(2.W)  // lock type
  val cache = UInt(4.W)  // cache type
  val prot  = UInt(3.W)  // protection type
}

class W extends Bundle {
  val id   = UInt(4.W)  // transaction ID
  val data = UInt(32.W) // write data
  val strb = UInt(4.W)  // byte enable
  val last = Bool()     // last beat of burst
}

class B extends Bundle {
  val id   = UInt(4.W) // transaction ID
  val resp = UInt(2.W) // response type
}

class ICacheAXI extends Bundle {
  val ar = Decoupled(new AR())
  val r  = Flipped(Decoupled(new R()))
}

class DCacheAXI extends Bundle {
  val ar = Decoupled(new AR())
  val r  = Flipped(Decoupled(new R()))
  val aw = Decoupled(new AW())
  val w  = Decoupled(new W())
  val b  = Flipped(Decoupled(new B()))
}

class AXIIO extends Bundle {
  val ar = Decoupled(new AR())         // read address channel
  val r  = Flipped(Decoupled(new R())) // read data channel
  val aw = Decoupled(new AW())         // write address channel
  val w  = Decoupled(new W())          // write data channel
  val b  = Flipped(Decoupled(new B())) // write response channel
}

class PreFetchICacheIO extends Bundle {
  val request = DecoupledIO(new Bundle {
    val addr = UInt(ADDR_WIDTH.W)
  })
}

class FetchICacheIO extends Bundle {
  val answer  = Flipped(DecoupledIO(Vec(4, UInt(INST_WIDTH.W))))
  val request = DecoupledIO(UInt(ADDR_WIDTH.W))
  val cango   = Output(Bool())
}

class Mem1DCacheIO extends Bundle {
  val request = DecoupledIO(new Bundle {
    val cached = Bool()
    val re     = Bool()
    val we     = Bool()
    val addr   = UInt(ADDR_WIDTH.W)
    val data   = UInt(DATA_WIDTH.W)
    val strb   = UInt(8.W)
  })
  val answer     = Flipped(DecoupledIO(UInt(DATA_WIDTH.W)))
  val answer_imm = Input(Bool())
  // val cango  = Output(Bool())
}

class Mem0DCacheIO extends Bundle {
  val request = DecoupledIO(new Bundle {
    val addr = UInt(ADDR_WIDTH.W)
  })
}
