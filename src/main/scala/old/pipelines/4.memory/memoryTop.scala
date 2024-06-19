package stages

import chisel3._
import chisel3.util._

import isa._
import bundles._
import const.ECodes
import const.Parameters._
import Funcs.Functions._
import const.memType

class MemoryTopIO extends StageBundle {
  val forward_data  = Output(new ForwardData)
  val load_complete = Output(Bool())
  val dCache        = new mem_dCache_IO
  val tlb           = new mem_TLB_IO
}

class MemoryTop extends Module {
  val io   = IO(new MemoryTopIO)
  val busy = WireDefault(false.B)
  val from = StageConnect(io.from, io.to, busy)
  val info = from._1
  FlushWhen(info, io.flush)

  io.tlb.va       := info.result
  io.tlb.mem_type := Mux(MemOpType.isread(info.op_type), memType.load, memType.store)

  val has_exc = info.exc_type =/= ECodes.NONE

  val dcache_saved_ans = RegInit(0.U(DATA_WIDTH.W))
  val complete         = RegInit(false.B)
  when(io.dCache.answer.fire) {
    dcache_saved_ans := io.dCache.answer.bits
    complete         := true.B
  }
  when(io.from.fire) {
    complete         := false.B
    dcache_saved_ans := 0.U
  }
  val finish = io.dCache.answer.fire || complete
  val mem    = Module(new Mmu).io
  mem.func_type := info.func_type
  mem.op_type   := info.op_type
  mem.result    := info.result
  mem.rd_value  := info.rd
  val is_mem      = info.func_type === FuncType.mem
  val mem_has_exc = mem.exc_type =/= ECodes.NONE
  io.dCache.request.valid := (mem.data_sram.en || mem.data_sram.we.orR) && !has_exc && !mem_has_exc
  when(ShiftRegister(info.pc, 1) === info.pc && ShiftRegister(finish, 1)) {
    // when getans and this stage is stall
    io.dCache.request.valid := false.B
  }
  io.dCache.request.bits.cached := io.tlb.cached
  io.dCache.request.bits.re     := mem.data_sram.en
  io.dCache.request.bits.we     := mem.data_sram.we.orR
  io.dCache.request.bits.addr   := io.tlb.pa
  io.dCache.request.bits.data   := mem.data_sram.wdata
  io.dCache.request.bits.strb   := mem.data_sram.we
  mem.data_sram.rdata           := Mux(io.dCache.answer.fire, io.dCache.answer.bits, dcache_saved_ans)
  io.dCache.answer.ready        := true.B
  busy                          := !finish && is_mem && !mem_has_exc
  when(io.dCache.answer_imm) { busy := false.B }

  val to_info = WireDefault(0.U.asTypeOf(new info))
  to_info        := info
  to_info.result := mem.data
  to_info.exc_type := Mux(
    has_exc,
    info.exc_type,
    Mux(mem_has_exc, mem.exc_type, Mux(is_mem, io.tlb.exc_type, ECodes.NONE)),
  )
  to_info.exc_vaddr := Mux(has_exc, info.exc_vaddr, Mux(mem_has_exc, mem.exc_vaddr, io.tlb.exc_vaddr))
  to_info.iswf      := Mux(to_info.exc_type =/= ECodes.NONE, false.B, info.iswf)
  FlushWhen(to_info, io.flush)
  io.to.bits := to_info

  io.flush_apply := to_info.exc_type =/= ECodes.NONE && io.to.valid && !info.bubble

  Forward(to_info, io.forward_data, from._2)
  io.load_complete := finish // && io.forward_tag && info.pc === io.forward_pc
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
