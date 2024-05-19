package stages

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._

class WriteBackTopIO extends StageBundle {
  val forward_data = Output(new ForwardData)
  val gr_write     = Output(new GRWrite)
  val debug_wb     = Output(new debug_wb)
}

class WriteBackTop extends Module {
  val io   = IO(new WriteBackTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)

  io.gr_write.we    := info.iswf
  io.gr_write.wmask := ALL_MASK.U
  io.gr_write.waddr := info.wfreg
  io.gr_write.wdata := info.result

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info := info
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.flush_apply := false.B

  Forward(to_info, io.forward_data)

  io.debug_wb.pc       := info.pc
  io.debug_wb.rf_we    := Fill(4, info.iswf && io.to.valid)
  io.debug_wb.rf_wnum  := info.wfreg
  io.debug_wb.rf_wdata := info.result
}
