package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import controller._
import config.Functions._

class Interface extends Bundle with Parameters {
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
class SignedDiv extends BlackBox with HasBlackBoxResource with Parameters {
  val io = IO(new Interface)
}

class UnsignedDiv extends BlackBox with HasBlackBoxResource with Parameters {
  val io = IO(new Interface)
}

class DIV_IO extends Bundle with Parameters {
  val div_op     = Input(DivOpType())
  val div_src1   = Input(UInt(DATA_WIDTH.W))
  val div_src2   = Input(UInt(DATA_WIDTH.W))
  val div_result = Output(UInt(DATA_WIDTH.W))

  val start    = Input(Bool())
  val complete = Output(Bool())
}

object Connect {
  def apply(div: Interface, top: DIV_IO, clock: Clock): Unit = {
    div.aclk                  := clock
    div.s_axis_dividend_tdata := top.div_src1
    div.s_axis_divisor_tdata  := top.div_src2
    val sent = Seq.fill(2)(RegInit(false.B))
    top.complete := div.m_axis_dout_tvalid

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

    div.s_axis_dividend_tvalid := top.start && !sent(0)
    div.s_axis_divisor_tvalid  := top.start && !sent(1)
  }
}

class DIV extends Module with Parameters {
  val io = IO(new DIV_IO)

  val signed_div   = Module(new SignedDiv())
  val unsigned_div = Module(new UnsignedDiv())

  Connect(signed_div.io, this.io, clock)
  Connect(unsigned_div.io, this.io, clock)

  io.div_result := MateDefault(
    io.div_op,
    0.U,
    List(
      DivOpType.s    -> signed_div.io.m_axis_dout_tdata(63, 32),
      DivOpType.u    -> unsigned_div.io.m_axis_dout_tdata(63, 32),
      DivOpType.smod -> signed_div.io.m_axis_dout_tdata(31, 0),
      DivOpType.umod -> unsigned_div.io.m_axis_dout_tdata(31, 0),
    ),
  )
}
