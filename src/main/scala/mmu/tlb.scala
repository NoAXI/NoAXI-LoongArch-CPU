package mmu

import chisel3._
import chisel3.util._

import const.tlbConst._
import const.Parameters._
import bundles._
import const.ECodes
import const.memType

class TLBIO extends Bundle {
  // from csr
  val csr = Flipped(new csr_TLB_IO)

  // act with mem
  val mem      = Flipped(new mem_TLB_IO)
  val exc_type = Output(ECodes())
  val pa       = Output(UInt(ADDR_WIDTH.W))
  val mat      = Output(UInt(2.W))
}

class TLB extends Module {
  val io = IO(new TLBIO)

  // Page Table Mapping Mode
  // from the 指令集手册
  val tlb       = RegInit(VecInit(Seq.fill(TLB_ENTRIES)(0.U.asTypeOf(new TLBEntry))))
  val tlb_found = WireDefault(false.B) // hit
  val found_ps  = WireDefault(0.U(6.W))
  val found     = WireDefault(0.U.asTypeOf(new TLBTransform))

  for (i <- 0 until TLB_ENTRIES) {
    val tlb_vppn = Mux(tlb(i).ps(3), tlb(i).vppn, tlb(i).vppn(18, 10) ## 0.U(10.W))
    val va_vppn  = Mux(tlb(i).ps(3), io.mem.va(31, 13), io.mem.va(31, 23) ## 0.U(10.W))
    when(
      (tlb(i).e) &&
        (tlb(i).g || tlb(i).asid === io.csr.asid.asid) &&
        (tlb_vppn === va_vppn),
    ) {
      tlb_found := true.B
      found_ps  := tlb(i).ps
      val page = found_ps =/= 0.U
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
      exc_type := io.mem.mem_type
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
    io.pa       := io.mem.va
    io.mat      := io.csr.crmd.datm // if fetch, then datf
    io.exc_type := ECodes.NONE
  }.elsewhen(direct_hitted) {
    // check if Direct mapping mode
    io.pa       := Cat(io.csr.dmw(direct_hittedway).pseg, io.mem.va(28, 0))
    io.mat      := io.csr.dmw(direct_hittedway).mat
    io.exc_type := ECodes.NONE
  }.otherwise {
    // Page Table Mapping Mode
    io.pa       := Mux(found_ps(3), Cat(found.ppn, io.mem.va(11, 0)), Cat(found.ppn(19, 9), io.mem.va(20, 0)))
    io.mat      := found.mat
    io.exc_type := Mux(io.mem.request, exc_type, ECodes.NONE)
  }
}
