package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.Parameters._
import Funcs.Functions._

class DecoderTopIO extends StageBundle {
  val forward_query = Output(new ForwardQuery)
  val forward_ans   = Input(new ForwardAns)
  val gr_write      = Input(new GRWrite)
}

class DecoderTop extends Module {
  val io   = IO(new DecoderTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)

  val rj   = info.inst(9, 5)
  val rk   = info.inst(14, 10)
  val rd   = info.inst(4, 0)
  val addr = Seq(rj, rk, rd)

  val gr_reg = Module(new GRReg).io
  gr_reg.raddr              := addr
  gr_reg.rf_bus             := io.gr_write
  io.forward_query.addr     := addr
  io.forward_query.ini_data := gr_reg.rdata
  busy                      := io.forward_ans.notld

  val decoder = Module(new Decoder).io
  decoder.inst := info.inst
  decoder.pc   := info.pc
  decoder.data := io.forward_ans.data

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.func_type := decoder.func_type
  to_info.op_type   := decoder.op_type
  to_info.isload    := decoder.isload
  to_info.imm       := decoder.imm
  to_info.src1      := decoder.src1
  to_info.src2      := decoder.src2
  to_info.rj        := io.forward_ans.data(0)
  to_info.rd        := io.forward_ans.data(2)
  to_info.iswf      := decoder.iswf && !info.bubble
  to_info.wfreg     := decoder.wfreg
  to_info.csr_iswf  := decoder.csr_iswf
  to_info.csr_addr := decoder.csr_wfreg
  to_info.exc_type  := Mux(info.exc_type =/= ECodes.NONE, info.exc_type, decoder.exc_type)
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble
}
