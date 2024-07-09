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

  // from stage0: get hitVec or isDirect
  val stage0 = Flipped(new Stage0TLBIO)

  // from stage1: get pa and store type, get exception
  val stage1 = Flipped(new Stage1TLBIO)
}

class TLB(
    unitType: String,
) extends Module {
  assert(Seq("fetch", "memory").contains(unitType))

  val io = IO(new TLBIO)

  io.stage1.pa := 0.U

  val tlb       = RegInit(VecInit(Seq.fill(TLB_ENTRIES)(0.U.asTypeOf(new TLBEntry))))
  val tlb_found = WireDefault(false.B) // tlb_hit
  val tlbHitVec = WireDefault(VecInit(Seq.fill(TLB_ENTRIES)(false.B)))
  val found_ps  = WireDefault(0.U(6.W))
  val found     = WireDefault(0.U.asTypeOf(new TLBTransform))

  // get hitVec
  for (i <- 0 until TLB_ENTRIES) {
    val tlb_vppn = Mux(tlb(i).ps(3), tlb(i).vppn, tlb(i).vppn(18, 9))
    val va       = io.stage0.va
    val va_vppn  = Mux(tlb(i).ps(3), va(31, 13), va(31, 22))
    // is the vppn compare right?
    when(
      (tlb(i).e) &&
        (tlb(i).g || tlb(i).asid === io.csr.asid.asid) &&
        (tlb_vppn === va_vppn),
    ) {
      tlbHitVec(i) := true.B
    }
  }
  io.stage0.hitVec := tlbHitVec

  // Page Table Mapping Mode
  // from the isaBook
  for (i <- 0 until TLB_ENTRIES) {
    val page = Mux(tlb(i).ps(3), io.stage1.va(12), io.stage1.va(21))
    when(io.stage1.hitVec(i)) {
      tlb_found := true.B
      found_ps  := tlb(i).ps
      found.v   := tlb(i).v(page)
      found.d   := tlb(i).d(page)
      found.mat := tlb(i).mat(page)
      found.plv := tlb(i).plv(page)
      found.ppn := tlb(i).ppn(page)
    }
  }

  val excEn   = WireDefault(false.B)
  val excType = WireDefault(ECodes.NONE)

  when(!tlb_found) {
    // tlb refill exception
    excEn   := true.B
    excType := ECodes.TLBR
  }

  when(!found.v) {
    when(io.stage0.memType.orR) {
      excEn   := true.B
      excType := io.stage0.memType // PIL PIS or PIF
    }.elsewhen(io.csr.crmd.plv > found.plv) { // TODO: > can be improved?
      excEn   := true.B
      excType := ECodes.PPI // page privilege illegal
    }.elsewhen(io.stage0.memType === memType.store && !found.d) {
      excEn   := true.B
      excType := ECodes.PME // page maintain exception
    }
  }

  // Direct mapping mode
  val plv_match =
    VecInit.tabulate(2)(i =>
      (io.csr.dmw(i).plv0 && io.csr.crmd.plv === 0.U) || (io.csr.dmw(i).plv3 && io.csr.crmd.plv === 3.U),
    )
  val direct_hit       = VecInit.tabulate(2)(i => io.csr.dmw(i).vseg === io.stage0.va(31, 29) && plv_match(i))
  val direct_hitted    = direct_hit.reduce(_ || _)
  val direct_hittedway = PriorityEncoder(direct_hit)

  // if direct, pa=va, no other things to worry
  io.stage0.isDirect := false.B
  io.stage0.directpa := 0.U
  when(io.csr.is_direct) {
    io.stage0.isDirect := true.B
    io.stage0.directpa := io.stage0.va & 0x1fffffff.U
    if (unitType == "fetch") {
      io.stage1.cached := ShiftRegister(io.csr.crmd.datf(0), 1) // send cached info when at stage1
    } else {
      io.stage1.cached := ShiftRegister(io.csr.crmd.datm(0), 1) // send cached info when at stage1
    }
    io.stage1.exception := ShiftRegister(0.U.asTypeOf(new Exception), 1)
  }.elsewhen(direct_hitted) {
    // check if Direct mapping mode
    io.stage0.isDirect  := true.B
    io.stage0.directpa  := Cat(io.csr.dmw(direct_hittedway).pseg, io.stage0.va(28, 0))
    io.stage1.cached    := ShiftRegister(io.csr.dmw(direct_hittedway).mat(0), 1)
    io.stage1.exception := ShiftRegister(0.U.asTypeOf(new Exception), 1)
  }.otherwise {
    // Page Table Mapping Mode
    io.stage1.pa := Mux(found_ps(3), Cat(found.ppn, io.stage1.va(11, 0)), Cat(found.ppn(19, 9), io.stage1.va(20, 0)))
    io.stage1.cached            := found.mat(0)
    io.stage1.exception.en      := excEn
    io.stage1.exception.excType := excType
  }
  io.stage1.exception.excVAddr := io.stage1.va

  if (Config.debug_on) {
    dontTouch(direct_hit)
  }
}
