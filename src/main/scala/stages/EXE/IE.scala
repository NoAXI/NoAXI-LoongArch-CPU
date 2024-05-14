package stages

import chisel3._
import chisel3.util._

import isa._
import csr._
import config._
import controller._
import config.Functions._

class IE_IO extends Bundle with Parameters {
  val from        = Flipped(DecoupledIO(new info))
  val to          = DecoupledIO(new info)
  val flush_en    = Input(Bool())
  val flush_apply = Output(UInt(5.W))

  val this_exc = Output(Bool())
  val has_exc  = Input(Bool())

  val es          = Output(new hazardData)
  val csr_es      = Output(new hazardData)
  val ds_reg_info = Input(new dsRegInfo)

  // act with dcache
  val exe     = Flipped(DecoupledIO(UInt(32.W)))
  val request = Output(Bool())
  val finish  = Output(Bool())
  val ren     = Output(Bool())
  val wen     = Output(Bool())
  val size    = Output(UInt(32.W))
  val wstrb   = Output(UInt(8.W))
  val addr    = Output(UInt(ADDR_WIDTH.W))
  val wdata   = Output(UInt(DATA_WIDTH.W))
}

class IE extends Module with Parameters {
  val io = IO(new IE_IO)

  // 与上一流水级握手，获取上一流水级信息
  val info = ConnectGetBus(io.from, io.to)
  when(io.flush_en || io.has_exc) {
    info := WireDefault(0.U.asTypeOf(new info))
  }
  io.flush_apply := 0.U
  // io.this_exc := info.this_exc

  val es_mem_re = info.func_type === FuncType.mem && MemOpType.isread(info.op_type)
  val es_mem_we = info.func_type === FuncType.mem && !es_mem_re

  // when(es_mem_re && io.ds_reg_info.addr.exists(_ === info.dest)) {
  //   io.from.ready := false.B
  //   info          := WireDefault(0.U.asTypeOf(new info))
  // }
  when (io.ds_reg_info.addr.exists(_ === info.dest) && io.exe.valid) {
    io.from.ready := false.B
    info          := WireDefault(0.U.asTypeOf(new info))
  }

  when (es_mem_re && !io.exe.valid) {
    io.from.ready := false.B
    io.to.valid := false.B
  } 

  // 调用DIV获得运算结果
  val div = Module(new DIV)
  div.io.div_op   := info.op_type
  div.io.div_src1 := info.src1
  div.io.div_src2 := info.src2
  div.io.start    := info.func_type === FuncType.div
  val div_result = div.io.div_result

  val is_cal = WireDefault(true.B)

  when(info.func_type === FuncType.div) {
    io.from.ready := false.B
    is_cal        := false.B
    when(div.io.complete) {
      info.func_type := FuncType.nondiv
    }
  }

  // 调用MUL获得运算结果
  val mul = Module(new MUL)
  mul.io.mul_op   := info.op_type
  mul.io.mul_src1 := info.src1
  mul.io.mul_src2 := info.src2

  when(info.func_type === FuncType.mul) {
    io.from.ready  := false.B
    is_cal         := false.B
    info.func_type := FuncType.nonmul
  }

  // 调用ALU获得运算结果
  val alu = Module(new ALU)
  alu.io.alu_op   := info.op_type
  alu.io.alu_src1 := info.src1
  alu.io.alu_src2 := info.src2

  val result = MateDefault(
    info.func_type,
    alu.io.alu_result,
    List(
      FuncType.div    -> div.io.div_result,
      FuncType.nondiv -> div.io.div_result,
      FuncType.mul    -> mul.io.mul_result,
      FuncType.nonmul -> mul.io.mul_result,
      FuncType.csr    -> info.csr_val,
    ),
  )

  val exception = Mux(
    io.to.valid && es_mem_we && (!io.has_exc) || es_mem_re,
    MuxCase(
      ECodes.NONE,
      List(
        (info.op_type === MemOpType.writeh && (result(0) =/= "b0".U))     -> ECodes.ALE,
        (info.op_type === MemOpType.writew && (result(1, 0) =/= "b00".U)) -> ECodes.ALE,
        (info.op_type === MemOpType.readh && (result(0) =/= "b0".U))      -> ECodes.ALE,
        (info.op_type === MemOpType.readhu && (result(0) =/= "b0".U))     -> ECodes.ALE,
        (info.op_type === MemOpType.readw && (result(1, 0) =/= "b00".U))  -> ECodes.ALE,
        // (result(31) === 1.U) -> ECodes.ADEM,
      ),
    ),
    ECodes.NONE,
  )

  io.exe.ready := true.B
  io.request   := (es_mem_re || es_mem_we) && (!io.exe.valid && !ShiftRegister(io.exe.valid, 1)) // vivado专属傻呗版本
  io.finish    := true.B
  io.ren       := es_mem_re
  io.wen       := es_mem_we
  io.size      := 4.U // not sure
  io.wstrb := Mux(
    es_mem_we && (!io.has_exc) && (exception === ECodes.NONE),
    MateDefault(
      info.op_type,
      0.U,
      List(
        MemOpType.writeb -> ("b0001".U << result(1, 0)),
        MemOpType.writeh -> ("b0011".U << result(1, 0)),
        MemOpType.writew -> "b1111".U,
      ),
    ),
    0.U,
  )
  io.addr := result
  io.wdata := MateDefault(
    info.op_type,
    0.U,
    List(
      MemOpType.writeb -> Fill(4, info.rkd_value(7, 0)),
      MemOpType.writeh -> Fill(2, info.rkd_value(15, 0)),
      MemOpType.writew -> info.rkd_value,
    ),
  )

  io.this_exc := Mux(info.this_exc, info.this_exc, exception =/= ECodes.NONE)

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info            := info
  to_info.rdata      := io.exe.bits
  to_info.piece      := result(1, 0)
  to_info.result     := result
  to_info.is_wf      := info.is_wf && is_cal
  to_info.this_exc   := io.this_exc
  to_info.exc_type   := Mux(info.this_exc, info.exc_type, exception)
  to_info.wrong_addr := Mux(info.this_exc, info.wrong_addr, result)
  io.to.bits         := to_info

  // 前递
  io.es.we       := to_info.is_wf
  io.es.addr     := to_info.dest
  io.es.data     := to_info.result
  io.csr_es.we   := to_info.csr_we
  io.csr_es.addr := to_info.csr_addr
  io.csr_es.data := to_info.rkd_value
}
