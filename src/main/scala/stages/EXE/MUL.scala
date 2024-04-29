package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import settings._
import controller._
import config.Functions._

class SignedMul extends BlackBox with HasBlackBoxResource with Parameters {
  val io = IO(new Bundle {
    val CLK = Input(Clock())
    val CE  = Input(Bool())
    val A   = Input(UInt((DATA_WIDTH + 1).W))
    val B   = Input(UInt((DATA_WIDTH + 1).W))
    val P   = Output(UInt((DATA_WIDTH * 2 + 2).W))
  })
}

class MUL_IO extends Bundle with Parameters {
  val mul_op     = Input(MulOpType())
  val mul_src1   = Input(UInt(DATA_WIDTH.W))
  val mul_src2   = Input(UInt(DATA_WIDTH.W))
  val mul_result = Output(UInt(DATA_WIDTH.W))
}

class MUL extends Module with Parameters {
  val io = IO(new MUL_IO)

  if (CpuConfig.hasBlackBox) {
    val signed_mul = Module(new SignedMul)
    signed_mul.io.CLK := clock
    signed_mul.io.CE  := true.B
    when(io.mul_op === MulOpType.shigh || io.mul_op === MulOpType.slow) {
      signed_mul.io.A := Cat(io.mul_src1(DATA_WIDTH - 1), io.mul_src1)
      signed_mul.io.B := Cat(io.mul_src2(DATA_WIDTH - 1), io.mul_src2)
    }.otherwise {
      signed_mul.io.A := Cat(Fill(1, 0.U), io.mul_src1)
      signed_mul.io.B := Cat(Fill(1, 0.U), io.mul_src2)
    }

    io.mul_result := MateDefault(
      io.mul_op,
      0.U,
      List(
        MulOpType.shigh -> signed_mul.io.P(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.slow  -> signed_mul.io.P(DATA_WIDTH, 0),
        MulOpType.uhigh -> signed_mul.io.P(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.ulow  -> signed_mul.io.P(DATA_WIDTH - 1, 0),
      ),
    )
  } else {
    val signed_result   = (io.mul_src1.asSInt * io.mul_src2.asSInt).asUInt
    val unsigned_result = io.mul_src1 * io.mul_src2
    io.mul_result := MateDefault(
      io.mul_op,
      0.U,
      List(
        MulOpType.shigh -> signed_result(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.slow  -> signed_result(DATA_WIDTH, 0),
        MulOpType.uhigh -> unsigned_result(DATA_WIDTH * 2 - 1, DATA_WIDTH),
        MulOpType.ulow  -> unsigned_result(DATA_WIDTH - 1, 0),
      ),
    )
  }
}
