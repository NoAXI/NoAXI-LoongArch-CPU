package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.Parameters._
import Funcs.Functions._

class MemoryTopIO extends StageBundle {
  val forward_data  = Output(new ForwardData)
  val forward_tag   = Input(Bool())
  val forward_pc    = Input(UInt(ADDR_WIDTH.W))
  val load_complete = Output(Bool())
  val dCache        = new _mem_dCache_IO
}

class MemoryTop extends Module {
  val io   = IO(new MemoryTopIO)
  val busy = WireDefault(false.B)
  val info = StageConnect(io.from, io.to, busy)
  FlushWhen(info, io.flush)

  val complete = RegInit(false.B)
  when(io.dCache.answer.fire) { complete := true.B }
  when(io.from.fire) { complete := false.B }
  val finish = io.dCache.answer.fire || complete
  val mmu    = Module(new Mmu).io
  mmu.func_type := info.func_type
  mmu.op_type   := info.op_type
  mmu.result    := info.result
  mmu.rd_value  := info.rd
  // io.dCache.request_r.valid     := mmu.data_sram.en && busy
  // io.dCache.request_r.bits      := mmu.data_sram.addr
  // io.dCache.request_w.valid     := mmu.data_sram.we.orR && busy
  // io.dCache.request_w.bits.strb := mmu.data_sram.we
  // io.dCache.request_w.bits.addr := mmu.data_sram.addr
  // io.dCache.request_w.bits.data := mmu.data_sram.wdata
  io.dCache.request.valid     := (mmu.data_sram.en || mmu.data_sram.we.orR) && !finish
  io.dCache.request.bits.re   := mmu.data_sram.en
  io.dCache.request.bits.we   := mmu.data_sram.we
  io.dCache.request.bits.addr := mmu.data_sram.addr
  io.dCache.request.bits.data := mmu.data_sram.wdata
  io.dCache.request.bits.strb := mmu.data_sram.we
  mmu.data_sram.rdata         := io.dCache.answer.bits
  io.dCache.answer.ready      := true.B
  busy                        := !finish && info.func_type === FuncType.mem && mmu.exc_type === ECodes.NONE
  when(io.dCache.answer_imm) { busy := false.B }
  val has_exc = info.exc_type =/= ECodes.NONE

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info           := info
  to_info.isload    := false.B
  to_info.result    := mmu.data
  to_info.exc_type  := Mux(has_exc, info.exc_type, mmu.exc_type)
  to_info.exc_vaddr := Mux(has_exc, info.exc_vaddr, mmu.exc_vaddr)
  to_info.iswf      := Mux(to_info.exc_type =/= ECodes.NONE, false.B, info.iswf)
  FlushWhen(to_info, io.flush)
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble

  Forward(to_info, io.forward_data)
  io.load_complete := finish && io.forward_tag && info.pc === io.forward_pc
}
/*
因为写后读冲突需要处理ld指令写完寄存器后才能读
但是axi的“慢”，导致原先exe的ld_tag有可能没法传到mem，
解决：选择直接从forwarder连过来
有一个小问题：发现写后读冲突dec级的发现时间，需要早于mem级的写回时间（可以保证吗？）

如果这一个读指令耗时太长，不能让他发出多次询问(发现写指令同样存在该问题)
解决：在得到答案时就不发出请求了
问题：应该是“在得到答案‘后’就不发出请求了”,但是得到答案就立马流到下一流水级了，所以应该问题不大
问题很大，现在真的遇到这个问题了，原因在于前面一级可能处于某种原因需要等一会，导致这个答案不会立即流
解决：看前面是否fire，如果fire了就不发请求了

load_complete 不应该由 生成tag的前面的指令触发，要对应于发出ld_tag的指令

写内存操作的b响应通道真的可以不理吗？
如果现在有两条连续的st指令，第一条还没写完（即b通道未发出valid），第二条就发出请求，这应该会寄，而且如果此时需要读，那也得等写完才能读
解决方案：等到b发出valid
 */
