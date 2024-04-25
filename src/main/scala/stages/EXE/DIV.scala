package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import controller._
import config.Functions._

class SignedDiv extends BlackBox with HasBlackBoxResource with Parameters {
  val io = IO(new Bundle {
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
  })

//   addResource("/path/to/your/file.v")
}

class UnsignedDiv extends BlackBox with HasBlackBoxResource with Parameters {
  val io = IO(new Bundle {
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
  })

//   addResource("/path/to/your/file.v")
}

class DIV_IO extends Bundle with Parameters {
  val div_op     = Input(DivOpType())
  val div_src1   = Input(UInt(DATA_WIDTH.W))
  val div_src2   = Input(UInt(DATA_WIDTH.W))
  val div_result = Output(UInt(DATA_WIDTH.W))

//   val ready   = Output(Bool())
  val start    = Input(Bool())
  val complete = Output(Bool())
//   val running = Output(Bool())
}

class DIV extends Module with Parameters {
  val io = IO(new DIV_IO)

  val signed_div = Module(new SignedDiv())

  val sent = Seq.fill(2)(RegInit(false.B))
  signed_div.io.aclk                  := clock
  signed_div.io.s_axis_dividend_tdata := io.div_src1
  signed_div.io.s_axis_divisor_tdata  := io.div_src2

//   val running = RegInit(false.B)
//   running := io.start && !signed_div.io.m_axis_dout_tvalid

//   io.running := running
  io.complete := signed_div.io.m_axis_dout_tvalid

  when(signed_div.io.s_axis_dividend_tvalid && signed_div.io.s_axis_dividend_tready) {
    sent(0) := true.B
  }.elsewhen(signed_div.io.m_axis_dout_tvalid) {
    sent(0) := false.B
  }

  when(signed_div.io.s_axis_divisor_tvalid && signed_div.io.s_axis_divisor_tready) {
    sent(1) := true.B
  }.elsewhen(signed_div.io.m_axis_dout_tvalid) {
    sent(1) := false.B
  }

  signed_div.io.s_axis_dividend_tvalid := io.start && !sent(0)
  signed_div.io.s_axis_divisor_tvalid  := io.start && !sent(1)

  val unsigned_div = Module(new UnsignedDiv())

  val sent2 = Seq.fill(2)(RegInit(false.B))
  unsigned_div.io.aclk                  := clock
  unsigned_div.io.s_axis_dividend_tdata := io.div_src1
  unsigned_div.io.s_axis_divisor_tdata  := io.div_src2

//   val running = RegInit(false.B)
//   running := io.start && !signed_div.io.m_axis_dout_tvalid

//   io.running := running
  io.complete := unsigned_div.io.m_axis_dout_tvalid

  when(unsigned_div.io.s_axis_dividend_tvalid && unsigned_div.io.s_axis_dividend_tready) {
    sent2(0) := true.B
  }.elsewhen(unsigned_div.io.m_axis_dout_tvalid) {
    sent2(0) := false.B
  }

  when(unsigned_div.io.s_axis_divisor_tvalid && unsigned_div.io.s_axis_divisor_tready) {
    sent2(1) := true.B
  }.elsewhen(unsigned_div.io.m_axis_dout_tvalid) {
    sent2(1) := false.B
  }

  unsigned_div.io.s_axis_dividend_tvalid := io.start && !sent2(0)
  unsigned_div.io.s_axis_divisor_tvalid  := io.start && !sent2(1)

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
