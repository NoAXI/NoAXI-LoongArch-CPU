package memory.tlb

import chisel3._
import chisel3.util._

import const._
import const.tlbConst._
import const.Parameters._
import bundles._
import isa.TlbOpType
import upickle.default

class TLBIO extends Bundle {
  // from csr
  val csr = Flipped(new CSRTLBIO)

  // from stage0: get hitVec or isDirect
  val stage0 = Vec(2, Flipped(new Stage0TLBIO))

  // from stage1: get pa and store type, get exception
  val stage1 = Vec(2, Flipped(new Stage1TLBIO))

  // tlb insts
  val commit = Flipped(new commitTLBIO)
}

class TLB extends Module {
  val io = IO(new TLBIO)

  for (i <- 0 until 2) {
    io.stage1(i).pa := 0.U
  }

  val tlb       = RegInit(VecInit(Seq.fill(TLB_ENTRIES)(0.U.asTypeOf(new TLBEntry))))
  val tlb_found = WireDefault(VecInit(Seq.fill(2)(false.B))) // tlb_hit
  val tlbHitVec = WireDefault(VecInit(Seq.fill(2)(VecInit(Seq.fill(TLB_ENTRIES)(false.B)))))
  val found_ps  = WireDefault(VecInit(Seq.fill(2)(0.U(6.W))))
  val found     = WireDefault(VecInit(Seq.fill(2)(0.U.asTypeOf(new TLBTransform))))

  // tlb insts
  val is_tlb_refill = io.csr.estat.ecode === ECodes.TLBR
  val tlb_index     = io.csr.tlbidx.index
  val tlbidx        = io.csr.tlbidx
  val tlbehi        = io.csr.tlbehi
  val tlbelo0       = io.csr.tlbelo0
  val tlbelo1       = io.csr.tlbelo1
  val asid          = io.csr.asid

  io.csr.tlbwe    := false.B
  io.csr.opType   := 0.U
  io.csr.hitted   := false.B
  io.csr.hitIndex := 0.U
  io.csr.tlbe     := 0.U.asTypeOf(new TLBEntry)

  when(io.commit.en) {
    switch(io.commit.opType) {
      is(TlbOpType.srch) {
        for (i <- 0 until TLB_ENTRIES) {
          io.csr.tlbwe  := true.B
          io.csr.opType := io.commit.opType
          // tlb.ps = 12 / 21
          // 12: 00 1100
          // 21: 01 0101
          val tlb_vppn = Mux(tlb(i).ps(3), tlb(i).vppn, tlb(i).vppn(18, 9))
          val va_vppn  = Mux(tlb(i).ps(3), io.commit.va(31, 13), io.commit.va(31, 22))
          when(
            (tlb(i).e) &&
              (tlb(i).g || tlb(i).asid === io.csr.asid.asid) &&
              (tlb_vppn === va_vppn),
          ) {
            io.csr.tlbe     := tlb(i)
            io.csr.hitIndex := i.U
            io.csr.hitted   := true.B
          }
        }
      }

      is(TlbOpType.rd) {
        io.csr.tlbwe  := true.B
        io.csr.opType := io.commit.opType
        io.csr.tlbe   := tlb(tlb_index)
      }

      is(TlbOpType.wr) {
        tlb(tlb_index).ps   := tlbidx.ps
        tlb(tlb_index).e    := !tlbidx.ne || is_tlb_refill // when at tlb refill, we fill a valid entry
        tlb(tlb_index).vppn := tlbehi.vppn

        tlb(tlb_index).g   := tlbelo0.g && tlbelo1.g
        tlb(tlb_index).ppn := VecInit(Seq(tlbelo0.ppn, tlbelo1.ppn))
        tlb(tlb_index).v   := VecInit(Seq(tlbelo0.v, tlbelo1.v))
        tlb(tlb_index).plv := VecInit(Seq(tlbelo0.plv, tlbelo1.plv))
        tlb(tlb_index).mat := VecInit(Seq(tlbelo0.mat, tlbelo1.mat))
        tlb(tlb_index).d   := VecInit(Seq(tlbelo0.d, tlbelo1.d))

        tlb(tlb_index).asid := asid.asid
      }
      // /|\            /|\
      //  |   the same   |
      // \|/            \|/
      is(TlbOpType.fill) {
        // tlb_index random chose
        val refill_index = RegInit(0.U(TLB_INDEX_LEN.W))
        refill_index := refill_index + 1.U

        tlb(refill_index).ps   := tlbidx.ps
        tlb(refill_index).e    := !tlbidx.ne || is_tlb_refill // when at tlb refill, we fill a valid entry
        tlb(refill_index).vppn := tlbehi.vppn

        tlb(refill_index).g   := tlbelo0.g && tlbelo1.g
        tlb(refill_index).ppn := VecInit(Seq(tlbelo0.ppn, tlbelo1.ppn))
        tlb(refill_index).v   := VecInit(Seq(tlbelo0.v, tlbelo1.v))
        tlb(refill_index).plv := VecInit(Seq(tlbelo0.plv, tlbelo1.plv))
        tlb(refill_index).mat := VecInit(Seq(tlbelo0.mat, tlbelo1.mat))
        tlb(refill_index).d   := VecInit(Seq(tlbelo0.d, tlbelo1.d))

        tlb(refill_index).asid := asid.asid
      }

      is(TlbOpType.inv) {
        switch(io.commit.op) {
          is(0.U) {
            for (i <- 0 until TLB_ENTRIES) {
              tlb(i) := 0.U.asTypeOf(new TLBEntry)
            }
          }

          is(1.U) {
            for (i <- 0 until TLB_ENTRIES) {
              tlb(i) := 0.U.asTypeOf(new TLBEntry)
            }
          }

          is(2.U) {
            for (i <- 0 until TLB_ENTRIES) {
              when(tlb(i).g) {
                tlb(i) := 0.U.asTypeOf(new TLBEntry)
              }
            }
          }

          is(3.U) {
            for (i <- 0 until TLB_ENTRIES) {
              when(!tlb(i).g) {
                tlb(i) := 0.U.asTypeOf(new TLBEntry)
              }
            }
          }

          is(4.U) {
            for (i <- 0 until TLB_ENTRIES) {
              when(!tlb(i).g && tlb(i).asid === io.commit.inv.asid) {
                tlb(i) := 0.U.asTypeOf(new TLBEntry)
              }
            }
          }

          is(5.U) {
            for (i <- 0 until TLB_ENTRIES) {
              val tlb_vppn = Mux(tlb(i).ps(3), tlb(i).vppn, tlb(i).vppn(18, 9))
              val va_vppn  = Mux(tlb(i).ps(3), io.commit.va(31, 13), io.commit.va(31, 22))
              when(!tlb(i).g && tlb(i).asid === io.commit.inv.asid && tlb_vppn === va_vppn) {
                tlb(i) := 0.U.asTypeOf(new TLBEntry)
              }
            }
          }

          is(6.U) {
            for (i <- 0 until TLB_ENTRIES) {
              val tlb_vppn = Mux(tlb(i).ps(3), tlb(i).vppn, tlb(i).vppn(18, 9))
              val va_vppn  = Mux(tlb(i).ps(3), io.commit.va(31, 13), io.commit.va(31, 22))
              when((tlb(i).g || tlb(i).asid === io.commit.inv.asid) && tlb_vppn === va_vppn) {
                tlb(i) := 0.U.asTypeOf(new TLBEntry)
              }
            }
          }
        }
      }
    }
  }

  // get hitVec
  for (i <- 0 until TLB_ENTRIES) {
    for (j <- 0 until 2) {
      val tlb_vppn = Mux(tlb(i).ps(3), tlb(i).vppn, tlb(i).vppn(18, 9))
      val va       = io.stage0(j).va
      val va_vppn  = Mux(tlb(i).ps(3), va(31, 13), va(31, 22))
      // is the vppn compare right?
      when(
        (tlb(i).e) &&
          (tlb(i).g || tlb(i).asid === asid.asid) &&
          (tlb_vppn === va_vppn),
      ) {
        tlbHitVec(j)(i) := true.B
      }
    }
  }
  for (i <- 0 until 2) {
    io.stage0(i).hitVec := tlbHitVec(i)
  }

  // Page Table Mapping Mode
  // from the isaBook
  for (i <- 0 until TLB_ENTRIES) {
    for (j <- 0 until 2) {
      val page = Mux(tlb(i).ps(3), io.stage1(j).va(12), io.stage1(j).va(21))
      when(io.stage1(j).hitVec(i)) {
        tlb_found(j) := true.B
        found_ps(j)  := tlb(i).ps
        found(j).v   := tlb(i).v(page)
        found(j).d   := tlb(i).d(page)
        found(j).mat := tlb(i).mat(page)
        found(j).plv := tlb(i).plv(page)
        found(j).ppn := tlb(i).ppn(page)
      }
    }
  }

  val excEn   = WireDefault(VecInit(Seq.fill(2)(false.B)))
  val excType = WireDefault(VecInit(Seq.fill(2)(ECodes.NONE)))

  for (j <- 0 until 2) {
    when(!tlb_found(j)) {
      // tlb refill exception
      excEn(j)   := true.B
      excType(j) := ECodes.TLBR
    }

    when(!found(j).v) {
      when(io.stage0(j).memType.orR) {
        excEn(j)   := true.B
        excType(j) := io.stage0(j).memType // PIL PIS or PIF
      }.elsewhen(io.csr.crmd.plv > found(j).plv) { // TODO: > can be improved?
        excEn(j)   := true.B
        excType(j) := ECodes.PPI // page privilege illegal
      }.elsewhen(io.stage0(j).memType === memType.store && !found(j).d) {
        excEn(j)   := true.B
        excType(j) := ECodes.PME // page maintain exception
      }
    }

    // Direct mapping mode
    val plv_match =
      VecInit.tabulate(2)(i => (io.csr.dmw(i).plv0 && io.csr.crmd.plv === 0.U) || (io.csr.dmw(i).plv3 && io.csr.crmd.plv === 3.U))
    val direct_hit       = VecInit.tabulate(2)(i => io.csr.dmw(i).vseg === io.stage0(j).va(31, 29) && plv_match(i))
    val direct_hitted    = direct_hit.reduce(_ || _)
    val direct_hittedway = PriorityEncoder(direct_hit)

    // if direct, pa=va, no other things to worry
    io.stage0(j).isDirect := false.B
    io.stage0(j).directpa := 0.U
    when(io.csr.is_direct) {
      io.stage0(j).isDirect := true.B
      io.stage0(j).directpa := io.stage0(j).va & 0x1fffffff.U
      when(!io.stage0(j).unitType) {
        io.stage1(j).cached := ShiftRegister(io.csr.crmd.datf(0), 1) // send cached info when at stage1
      }.otherwise {
        io.stage1(j).cached := ShiftRegister(io.csr.crmd.datm(0), 1) // send cached info when at stage1
      }
      io.stage1(j).exception := ShiftRegister(0.U.asTypeOf(new ExcInfo), 1)
    }.elsewhen(ShiftRegister(direct_hitted, 1)) {
      // check if Direct mapping mode
      io.stage0(j).isDirect  := true.B
      io.stage0(j).directpa  := Cat(io.csr.dmw(direct_hittedway).pseg, io.stage0(j).va(28, 0))
      io.stage1(j).cached    := ShiftRegister(io.csr.dmw(direct_hittedway).mat(0), 1)
      io.stage1(j).exception := ShiftRegister(0.U.asTypeOf(new ExcInfo), 1)
    }.otherwise {
      // Page Table Mapping Mode
      io.stage1(j).pa                := Mux(found_ps(j)(3), Cat(found(j).ppn, io.stage1(j).va(11, 0)), Cat(found(j).ppn(19, 9), io.stage1(j).va(20, 0)))
      io.stage1(j).cached            := found(j).mat(0)
      io.stage1(j).exception.en      := excEn(j)
      io.stage1(j).exception.excType := excType(j)
    }
    io.stage1(j).exception.excVAddr := io.stage1(j).va

    if (Config.debug_on) {
      dontTouch(direct_hit)
    }
  }
}
