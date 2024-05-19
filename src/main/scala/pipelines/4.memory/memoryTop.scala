package stages

import chisel3._
import chisel3.util._

import bundles._
import const.ECodes
import const.Parameters._
import Funcs.Functions._

class MemoryTopIO extends StageBundle {
  val data_sram     = new data_sramIO
  val forward_data  = Output(new ForwardData)
  val load_complete = Output(Bool())
}

class MemoryTop extends Module {
  val io   = IO(new MemoryTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)

  val mmu = Module(new Mmu).io
  mmu.func_type       := info.func_type
  mmu.op_type         := info.op_type
  mmu.result          := info.result
  mmu.rd_value        := info.rd
  io.data_sram.en     := mmu.data_sram.en
  io.data_sram.we     := mmu.data_sram.we
  io.data_sram.addr   := mmu.data_sram.addr
  io.data_sram.wdata  := mmu.data_sram.wdata
  mmu.data_sram.rdata := io.data_sram.rdata
  busy                := mmu.busy && info.pc =/= ShiftRegister(info.pc, 1)

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info        := info
  to_info.isload := false.B
  to_info.result := mmu.data
  to_info.exc_type  := Mux(info.exc_type =/= ECodes.NONE, info.exc_type, mmu.exc_type)
  when(io.flush) {
    to_info        := 0.U.asTypeOf(new info)
    to_info.bubble := true.B
  }
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble

  Forward(to_info, io.forward_data)
  io.load_complete := ShiftRegister(busy, 1) && info.ld_tag // can change to busy then not busy status
}
