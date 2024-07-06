package memory.tlb

import chisel3._
import chisel3.util._

import const._
import const.tlbConst._
import const.Parameters._
import bundles._

class TLBIO extends Bundle {
  // from csr
  val csr = Flipped(new CSRTLBIO)

  // from preFetch
  val preFetch = Flipped(new PreFetchTLBIO)

  // from fetch
  val fetch = Flipped(new FetchTLBIO)

  // from exe
  // val exe = Flipped(new exe_TLB_IO)

  // act with mem
  val mem = Flipped(new MemTLBIO)
}

class TLB extends Module {
  val io = IO(new TLBIO)

  io.fetch.pa     := ShiftRegister(io.preFetch.va, 1)
  io.fetch.cached := true.B

  val tlb       = RegInit(VecInit(Seq.fill(TLB_ENTRIES)(0.U.asTypeOf(new TLBEntry))))
  val tlb_found = WireDefault(false.B) // tlb_hit
  val found_ps  = WireDefault(0.U(6.W))
  val found     = WireDefault(0.U.asTypeOf(new TLBTransform))

  // TLB Insts, 复用命中逻辑
  // Page Table Mapping Mode
  // from the 指令集手册
  for (i <- 0 until TLB_ENTRIES) {
    val tlb_vppn = Mux(tlb(i).ps(3), tlb(i).vppn, tlb(i).vppn(18, 9))
    // val va       = Mux(io.exe.tlb_en, io.csr.tlbehi.vppn, io.mem.va)
    val va      = io.mem.va
    val va_vppn = Mux(tlb(i).ps(3), va(31, 13), va(31, 22))
    // is the vppn compare right?
    val page = Mux(tlb(i).ps(3), va(12), va(21))
    when(
      (tlb(i).e) &&
        (tlb(i).g || tlb(i).asid === io.csr.asid.asid) &&
        (tlb_vppn === va_vppn),
    ) {
      tlb_found := true.B
      found_ps  := tlb(i).ps
      found.v   := tlb(i).v(page)
      found.d   := tlb(i).d(page)
      found.mat := tlb(i).mat(page)
      found.plv := tlb(i).plv(page)
      found.ppn := tlb(i).ppn(page)
    }
  }

  val exc_type = WireDefault(ECodes.NONE)

  when(!tlb_found) {
    // tlb refill exception
    exc_type := ECodes.TLBR
  }

  when(!found.v) {
    when(io.mem.mem_type.orR) {
      exc_type := io.mem.mem_type // PIL PIS or PIF
    }.elsewhen(io.csr.crmd.plv > found.plv) { // TODO: > can be improved?
      exc_type := ECodes.PPI // page privilege illegal
    }.elsewhen(io.mem.mem_type === memType.store && !found.d) {
      exc_type := ECodes.PME // page maintain exception
    }
  }

  // Direct mapping mode
  val plv_match =
    VecInit.tabulate(2)(i =>
      (io.csr.dmw(i).plv0 && io.csr.crmd.plv === 0.U) || (io.csr.dmw(i).plv3 && io.csr.crmd.plv === 3.U),
    )
  val direct_hit       = VecInit.tabulate(2)(i => io.csr.dmw(i).vseg === io.mem.va(31, 29) && plv_match(i))
  val direct_hitted    = direct_hit.reduce(_ || _)
  val direct_hittedway = PriorityEncoder(direct_hit)

  // if direct, pa=va, no other things to worry
  when(io.csr.is_direct) {
    io.mem.pa       := io.mem.va & 0x1fffffff.U
    io.mem.cached   := io.csr.crmd.datm(0) // if fetch, then datf
    io.mem.exc_type := ECodes.NONE
  }.elsewhen(direct_hitted) {
    // check if Direct mapping mode
    io.mem.pa       := Cat(io.csr.dmw(direct_hittedway).pseg, io.mem.va(28, 0))
    io.mem.cached   := io.csr.dmw(direct_hittedway).mat(0)
    io.mem.exc_type := ECodes.NONE
  }.otherwise {
    // Page Table Mapping Mode
    io.mem.pa       := Mux(found_ps(3), Cat(found.ppn, io.mem.va(11, 0)), Cat(found.ppn(19, 9), io.mem.va(20, 0)))
    io.mem.cached   := found.mat(0)
    io.mem.exc_type := exc_type
  }
  io.mem.exc_vaddr := io.mem.va

  if (Config.debug_on) {
    dontTouch(direct_hit)
  }
}
