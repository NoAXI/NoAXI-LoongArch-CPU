package pipeline

import chisel3._
import chisel3.util._

import isa._
import const._
import bundles._
import func.Functions._
import const.Parameters._

class Interface extends Bundle {
  val aclk = Input(Clock())
  // 被除数
  val s_axis_dividend_tvalid = Input(Bool())
  val s_axis_dividend_tready = Output(Bool())
  val s_axis_dividend_tdata  = Input(UInt(DATA_WIDTH.W))
  // 除数
  val s_axis_divisor_tvalid = Input(Bool())
  val s_axis_divisor_tready = Output(Bool())
  val s_axis_divisor_tdata  = Input(UInt(DATA_WIDTH.W))
  // 结果
  val m_axis_dout_tvalid = Output(Bool())
  val m_axis_dout_tdata  = Output(UInt((DATA_WIDTH * 2).W))
}

//以下两个元件的名字必须是这两个
class SignedDiv extends BlackBox with HasBlackBoxResource {
  val io = IO(new Interface)
}

class UnsignedDiv extends BlackBox with HasBlackBoxResource {
  val io = IO(new Interface)
}

class DivIO extends Bundle {
  val op_type  = Input(DivOpType())
  val src1     = Input(UInt(DATA_WIDTH.W))
  val src2     = Input(UInt(DATA_WIDTH.W))
  val result   = Output(UInt(DATA_WIDTH.W))
  val running  = Input(Bool())
  val complete = Output(Bool())
}

object Connect {
  def apply(div: Interface, top: DivIO, clock: Clock): Unit = {
    div.aclk                  := clock
    div.s_axis_dividend_tdata := top.src1
    div.s_axis_divisor_tdata  := top.src2
    val sent = Seq.fill(2)(RegInit(false.B))

    when(div.s_axis_dividend_tvalid && div.s_axis_dividend_tready) {
      sent(0) := true.B
    }.elsewhen(div.m_axis_dout_tvalid) {
      sent(0) := false.B
    }

    when(div.s_axis_divisor_tvalid && div.s_axis_divisor_tready) {
      sent(1) := true.B
    }.elsewhen(div.m_axis_dout_tvalid) {
      sent(1) := false.B
    }

    div.s_axis_dividend_tvalid := top.running && !sent(0)
    div.s_axis_divisor_tvalid  := top.running && !sent(1)
    when(div.m_axis_dout_tvalid) {
      div.s_axis_dividend_tvalid := false.B
      div.s_axis_divisor_tvalid  := false.B
    }
  }
}

class Div extends Module {
  val io = IO(new DivIO)

  if (Config.hasBlackBox) {
    val signed_div   = Module(new SignedDiv())
    val unsigned_div = Module(new UnsignedDiv())

    Connect(signed_div.io, this.io, clock)
    Connect(unsigned_div.io, this.io, clock)

    io.complete := Mux(
      DivOpType.signed(io.op_type),
      signed_div.io.m_axis_dout_tvalid,
      unsigned_div.io.m_axis_dout_tvalid,
    )

    io.result := MateDefault(
      io.op_type,
      0.U,
      List(
        DivOpType.s    -> signed_div.io.m_axis_dout_tdata(63, 32),
        DivOpType.u    -> unsigned_div.io.m_axis_dout_tdata(63, 32),
        DivOpType.smod -> signed_div.io.m_axis_dout_tdata(31, 0),
        DivOpType.umod -> unsigned_div.io.m_axis_dout_tdata(31, 0),
      ),
    )
  } else {
    val cnt = RegInit(0.U(log2Ceil(Config.divClockNum + 1).W))
    cnt := MuxCase(
      cnt,
      Seq(
        io.running  -> (cnt + 1.U),
        io.complete -> 0.U,
      ),
    )

    val div_signed = DivOpType.signed(io.op_type)

    val dividend_signed = io.src1(31) & div_signed
    val divisor_signed  = io.src2(31) & div_signed

    val dividend_abs = Mux(dividend_signed, (-io.src1).asUInt, io.src1.asUInt)
    val divisor_abs  = Mux(divisor_signed, (-io.src2).asUInt, io.src2.asUInt)

    val quotient_signed  = (io.src1(31) ^ io.src2(31)) & div_signed
    val remainder_signed = io.src1(31) & div_signed

    val quotient_abs  = dividend_abs / divisor_abs
    val remainder_abs = dividend_abs - quotient_abs * divisor_abs

    val quotient  = RegInit(0.S(32.W))
    val remainder = RegInit(0.S(32.W))

    when(io.running) {
      quotient  := Mux(quotient_signed, (-quotient_abs).asSInt, quotient_abs.asSInt)
      remainder := Mux(remainder_signed, (-remainder_abs).asSInt, remainder_abs.asSInt)
    }

    io.complete := cnt >= Config.divClockNum.U
    io.result := MateDefault(
      io.op_type,
      0.U,
      List(
        DivOpType.s    -> quotient.asUInt,
        DivOpType.u    -> quotient.asUInt,
        DivOpType.smod -> remainder.asUInt,
        DivOpType.umod -> remainder.asUInt,
      ),
    )
  }
}
