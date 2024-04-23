package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import config.Functions._

class IE_IO extends Bundle with Parameters {
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)

  // ** to sram
  val data_sram_en    = Output(Bool())
  val data_sram_we    = Output(UInt(INST_WIDTH_B.W))
  val data_sram_addr  = Output(UInt(ADDR_WIDTH.W))
  val data_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IE extends Module with Parameters {
  val io = IO(new IE_IO)

  // 与上一流水级握手，获取上一流水级信息
  val info      = ConnectGetBus(io.from, io.to)
  val es_mem_we = info.func_type === FuncType.mem && info.op_type === MemOpType.write

  // 调用ALU获得运算结果
  val alu = Module(new ALU)
  alu.io.alu_op   := info.op_type
  alu.io.alu_src1 := info.src1
  alu.io.alu_src2 := info.src2
  val alu_result = alu.io.alu_result

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info        := info
  to_info.result := alu_result
  io.to.bits     := to_info

  io.data_sram_en    := true.B
  io.data_sram_we    := Mux(es_mem_we && io.to.valid, 15.U(4.W), 0.U)
  io.data_sram_addr  := alu_result
  io.data_sram_wdata := info.rkd_value
}
