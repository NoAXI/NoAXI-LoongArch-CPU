package pipeline

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.Parameters._
import func.Functions._

class Memory1AccessIO extends Bundle {
  val isMem    = Input(Bool())
  val op_type  = Input(UInt(5.W))
  val addr     = Input(UInt(DATA_WIDTH.W))
  val rd_value = Input(UInt(DATA_WIDTH.W))

  val wmask     = Output(UInt((DATA_WIDTH / 8).W))
  val wdata     = Output(UInt(DATA_WIDTH.W))
  val exc_type  = Output(ECodes())
  val exc_vaddr = Output(UInt(ADDR_WIDTH.W))
}

class Memory1Access extends Module {
  val io = IO(new Memory1AccessIO)

  val re    = MemOpType.isread(io.op_type) && io.isMem
  val we    = MemOpType.iswrite(io.op_type) && io.isMem
  val piece = io.addr(1, 0)

  io.exc_type := Mux(
    (re || we),
    MuxCase(
      ECodes.NONE,
      List(
        (io.op_type === MemOpType.writeh && (io.addr(0) =/= "b0".U))     -> ECodes.ALE,
        (io.op_type === MemOpType.writew && (io.addr(1, 0) =/= "b00".U)) -> ECodes.ALE,
        (io.op_type === MemOpType.readh && (io.addr(0) =/= "b0".U))      -> ECodes.ALE,
        (io.op_type === MemOpType.readhu && (io.addr(0) =/= "b0".U))     -> ECodes.ALE,
        (io.op_type === MemOpType.readw && (io.addr(1, 0) =/= "b00".U))  -> ECodes.ALE,
      ),
    ),
    ECodes.NONE,
  )
  io.exc_vaddr := io.addr

  io.wmask := Mux(
    we && io.exc_type === ECodes.NONE,
    MateDefault(
      io.op_type,
      0.U,
      List(
        MemOpType.writeb -> ("b0001".U << piece),
        MemOpType.writeh -> ("b0011".U << piece),
        MemOpType.writew -> "b1111".U,
      ),
    ),
    0.U,
  )
  io.wdata := MateDefault(
    io.op_type,
    0.U,
    List(
      MemOpType.writeb -> Fill(4, io.rd_value(7, 0)),
      MemOpType.writeh -> Fill(2, io.rd_value(15, 0)),
      MemOpType.writew -> io.rd_value,
    ),
  )
}
