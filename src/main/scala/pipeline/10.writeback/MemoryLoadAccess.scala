package pipeline

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.Parameters._
import func.Functions._

class MemoryLoadAccessIO extends Bundle {
  val op_type = Input(UInt(5.W))
  val addr    = Input(UInt(ADDR_WIDTH.W))
  val rdata   = Input(UInt(DATA_WIDTH.W))
  val data    = Output(UInt(DATA_WIDTH.W))
}

class MemoryLoadAccess extends Module {
  val io = IO(new MemoryLoadAccessIO)

  val re    = MemOpType.isread(io.op_type)
  val we    = !re
  val piece = io.addr(1, 0)

  val rdata_h = Extend(
    MateDefault(
      piece,
      0.U,
      List(
        "b00".U -> io.rdata(15, 0),
        "b10".U -> io.rdata(31, 16),
      ),
    ),
    DATA_WIDTH,
    MemOpType.signed(io.op_type),
  )
  val rdata_b = Extend(
    MateDefault(
      piece,
      0.U,
      List(
        "b00".U -> io.rdata(7, 0),
        "b01".U -> io.rdata(15, 8),
        "b10".U -> io.rdata(23, 16),
        "b11".U -> io.rdata(31, 24),
      ),
    ),
    DATA_WIDTH,
    MemOpType.signed(io.op_type),
  )
  val rdata = MuxCase(
    io.rdata,
    Seq(
      MemOpType.ish(io.op_type) -> rdata_h,
      MemOpType.isb(io.op_type) -> rdata_b,
    ),
  )

  io.data := rdata
}
