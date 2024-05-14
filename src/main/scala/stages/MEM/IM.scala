package stages

import chisel3._
import chisel3.util._

import isa._
import config._
import controller._
import config.Functions._

class IM_IO extends Bundle with Parameters {
  val from = Flipped(DecoupledIO(new info))
  val to   = DecoupledIO(new info)
  val flush_en    = Input(Bool())
  val flush_apply = Output(UInt(5.W))

  val this_exc = Output(Bool())
  val has_exc = Input(Bool())

  val ms = Output(new hazardData)
  val csr_ms = Output(new hazardData)

  // ** from data-sram
  // val data_sram_rdata = Input(UInt(INST_WIDTH.W))
}

class IM extends Module with Parameters {
  val io = IO(new IM_IO)

  // 与上一流水级握手，获取上一流水级信息
  val info = ConnectGetBus(io.from, io.to)
  when (io.flush_en || io.has_exc) {
    info          := WireDefault(0.U.asTypeOf(new info))
  }
  io.flush_apply := 0.U
  io.this_exc := info.this_exc

  // 传递信息
  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info := info
  to_info.result := Mux(
    info.func_type === FuncType.mem && MemOpType.isread(info.op_type),  // 是否是读操作
    MateDefault(
      info.op_type(2, 1),  // 看是h类型还是b类型
      info.rdata,  // 默认是readw
      List(
        MemOpType.b -> Extend(
          MateDefault(
            info.piece,
            0.U,
            List(
              "b00".U -> info.rdata(7, 0),
              "b01".U -> info.rdata(15, 8),
              "b10".U -> info.rdata(23, 16),
              "b11".U -> info.rdata(31, 24),
            ),
          ),
          DATA_WIDTH,
          info.op_type(0).asBool,
        ),
        MemOpType.h -> Extend(
          MateDefault(
            info.piece,
            0.U,
            List(
              "b00".U -> info.rdata(15, 0),
              "b10".U -> info.rdata(31, 16),
            ),
          ),
          DATA_WIDTH,
          info.op_type(0).asBool,
        ),
      ),
    ),
    info.result,
  )
  io.to.bits := to_info

  // 前递
  io.ms.we   := to_info.is_wf
  io.ms.addr := to_info.dest
  io.ms.data := to_info.result
  io.csr_ms.we := to_info.csr_we
  io.csr_ms.addr := to_info.csr_addr
  io.csr_ms.data := to_info.rkd_value
}
