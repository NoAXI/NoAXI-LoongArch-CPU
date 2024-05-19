package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.Parameters._
import Funcs.Functions._

class MmuIO extends Bundle {
  val func_type = Input(FuncType())
  val op_type   = Input(UInt(5.W))

  val data_sram = new data_sramIO

  val result   = Input(UInt(DATA_WIDTH.W))
  val rd_value = Input(UInt(DATA_WIDTH.W))

  val busy = Output(Bool())
  val data = Output(UInt(DATA_WIDTH.W))

  val exc_type = Output(ECodes())
}

class Mmu extends Module {
  val io = IO(new MmuIO)

  val re    = io.func_type === FuncType.mem && MemOpType.isread(io.op_type)
  val we    = io.func_type === FuncType.mem && !re
  val piece = io.result(1, 0)

  io.data_sram.en := true.B
  io.data_sram.we := Mux(
    we,
    MateDefault(
      io.op_type,
      0.U,
      List(
        MemOpType.writeb -> ("b0001".U << io.result(1, 0)),
        MemOpType.writeh -> ("b0011".U << io.result(1, 0)),
        MemOpType.writew -> "b1111".U,
      ),
    ),
    0.U,
  )
  io.data_sram.addr := io.result
  io.data_sram.wdata := MateDefault(
    io.op_type,
    0.U,
    List(
      MemOpType.writeb -> Fill(4, io.rd_value(7, 0)),
      MemOpType.writeh -> Fill(2, io.rd_value(15, 0)),
      MemOpType.writew -> io.rd_value,
    ),
  )

  val rdata_h = Extend(
    MateDefault(
      piece,
      0.U,
      List(
        "b00".U -> io.data_sram.rdata(15, 0),
        "b10".U -> io.data_sram.rdata(31, 16),
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
        "b00".U -> io.data_sram.rdata(7, 0),
        "b01".U -> io.data_sram.rdata(15, 8),
        "b10".U -> io.data_sram.rdata(23, 16),
        "b11".U -> io.data_sram.rdata(31, 24),
      ),
    ),
    DATA_WIDTH,
    MemOpType.signed(io.op_type),
  )
  val rdata = MuxCase(
    io.data_sram.rdata,
    Seq(
      MemOpType.ish(io.op_type) -> rdata_h,
      MemOpType.isb(io.op_type) -> rdata_b,
    ),
  )

  io.exc_type := Mux(
    (re || we),
    MuxCase(
      ECodes.NONE,
      List(
        (io.op_type === MemOpType.writeh && (io.result(0) =/= "b0".U))     -> ECodes.ALE,
        (io.op_type === MemOpType.writew && (io.result(1, 0) =/= "b00".U)) -> ECodes.ALE,
        (io.op_type === MemOpType.readh && (io.result(0) =/= "b0".U))      -> ECodes.ALE,
        (io.op_type === MemOpType.readhu && (io.result(0) =/= "b0".U))     -> ECodes.ALE,
        (io.op_type === MemOpType.readw && (io.result(1, 0) =/= "b00".U))  -> ECodes.ALE,
      ),
    ),
    ECodes.NONE,
  )

  io.busy := re
  io.data := Mux(re, rdata, io.result)
}
