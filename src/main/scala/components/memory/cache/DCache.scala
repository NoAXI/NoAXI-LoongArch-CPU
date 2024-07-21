package memory.cache

import chisel3._
import chisel3.util._

import bundles._
import func.Functions._
import const.cacheConst._
import const.Parameters._
import const.Config

import pipeline._
import os.stat

class DCacheIO extends Bundle {
  val axi  = new DCacheAXI
  val mem0 = Flipped(new Mem0DCacheIO)
  val mem1 = Flipped(new Mem1DCacheIO)
  val mem2 = Flipped(new Mem2DCacheIO)
}

class DCache extends Module {
  val io = IO(new DCacheIO)

  io.mem2.prevAwake := false.B

  val tagV =
    VecInit.fill(WAY_WIDTH)(Module(new xilinx_simple_dual_port_1_clock_ram_write_first((TAG_WIDTH + 1), LINE_WIDTH)).io)
  val tag   = VecInit.tabulate(WAY_WIDTH)(i => tagV(i).doutb(TAG_WIDTH, 1))
  val valid = VecInit.tabulate(WAY_WIDTH)(i => tagV(i).doutb(0))
  val data =
    VecInit.fill(WAY_WIDTH)(Module(new xilinx_simple_dual_port_1_clock_ram_write_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val dirty = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))
  val lru   = RegInit(VecInit.fill(LINE_WIDTH)(false.B))

  val ar      = RegInit(0.U.asTypeOf(new AR))
  val arvalid = RegInit(false.B)
  val rready  = RegInit(false.B)
  val aw      = RegInit(0.U.asTypeOf(new AW))
  val awvalid = RegInit(false.B)
  val w       = RegInit(0.U.asTypeOf(new W))
  val wvalid  = RegInit(false.B)
  val bready  = true.B

  // mem 0: send va
  val vaddr = io.mem0.addr
  for (i <- 0 until WAY_WIDTH) {
    data(i).clka  := clock
    data(i).addra := 0.U
    data(i).addrb := vaddr(11, 4)
    data(i).wea   := false.B
    data(i).dina  := 0.U
    tagV(i).clka  := clock
    tagV(i).addra := 0.U
    tagV(i).addrb := vaddr(11, 4)
    tagV(i).wea   := false.B
    tagV(i).dina  := 0.U
  }

  // mem 1: send va & exception
  val mem1paddr     = io.mem1.addr
  val cacheHitVecor = VecInit.tabulate(WAY_WIDTH)(i => tag(i) === mem1paddr(31, 12) && valid(i))
  io.mem1.hitVec := cacheHitVecor

  for (i <- 0 until WAY_WIDTH)
    data(i).addrb := mem1paddr(11, 4)

  // mem 2: get hitVec, act with D-Cache
  //   0           1               2             3              4            5              6
  val idle :: uncacheRead :: uncacheWrite :: checkdirty :: writeBack0 :: writeBack1 :: replaceLine :: Nil = Enum(7)

  val state  = RegInit(idle)
  val paddr  = io.mem2.request.bits.addr
  val cached = io.mem2.request.bits.cached
  val pa     = io.mem2.request.bits.addr
  val wdata  = io.mem2.request.bits.wdata
  val wstrb  = io.mem2.request.bits.wstrb
  val rwType = io.mem2.rwType
  val hitVec = io.mem2.hitVec

  val cacheHit    = hitVec.reduce(_ || _)
  val cacheHitWay = PriorityEncoder(hitVec)
  val hitdataline = Mux1H(hitVec, VecInit.tabulate(2)(i => data(i).doutb))
  val hitdata = MateDefault(
    pa(3, 2),
    0.U,
    Seq(
      0.U -> hitdataline(31, 0),
      1.U -> hitdataline(63, 32),
      2.U -> hitdataline(95, 64),
      3.U -> hitdataline(127, 96),
    ),
  )

  val imm_ansvalid   = WireDefault(false.B)
  val imm_cached_ans = WireDefault(0.U(DATA_WIDTH.W))
  val savedInfo      = RegInit(0.U.asTypeOf(new savedInfo))
  val count          = RegInit(1.U(4.W))
  val linedata       = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask          = RegInit(1.U(4.W))
  val wmove          = Mux1H(((DATA_WIDTH / 8 - 1) to 0 by -1).map(i => wmask(i) -> (i * 32).U))
  val w_data         = savedInfo.linedata(lru(savedInfo.index))

  switch(state) {
    is(idle) {
      imm_ansvalid := true.B
      when(io.mem2.request.fire) {
        when(cached) {
          when(cacheHit) {
            lru(pa(11, 4)) := !cacheHitWay
            imm_ansvalid   := true.B
            when(io.mem2.rwType) {
              // write
              data(cacheHitWay).wea   := true.B
              data(cacheHitWay).addra := pa(11, 4)
              data(cacheHitWay).dina := Merge(
                wstrb,
                hitdataline,
                wdata,
                pa(3, 2),
              )
              dirty(pa(11, 4))(cacheHitWay) := true.B
            }.otherwise {
              // read
              imm_cached_ans := hitdata
            }
          }.otherwise {
            imm_ansvalid       := false.B
            savedInfo.linedata := Seq(data(0).doutb, data(1).doutb)
            savedInfo.op       := rwType
            savedInfo.addr     := pa
            savedInfo.linetag  := Seq(tag(0), tag(1))
            savedInfo.wstrb    := wstrb
            savedInfo.wdata    := wdata
            state              := checkdirty
          }
        }.otherwise {
          imm_ansvalid := false.B
          when(!rwType) {
            arvalid := true.B
            rready  := false.B
            ar.addr := pa(ADDR_WIDTH - 1, 2) ## 0.U(2.W) // pay attention
            ar.len  := 0.U
            state   := uncacheRead
          }.otherwise {
            awvalid := true.B
            aw.addr := pa
            aw.len  := 0.U
            wvalid  := true.B
            w.strb  := wstrb
            w.data  := wdata
            w.last  := true.B
            state   := uncacheWrite
          }
        }
      }
    }

    is(uncacheRead) {
      when(io.axi.ar.valid) {
        when(io.axi.ar.ready) {
          arvalid := false.B
          rready  := true.B
        }
      }.elsewhen(io.axi.r.fire) {
        imm_ansvalid   := true.B
        imm_cached_ans := io.axi.r.bits.data
        state          := idle
      }
    }
    is(uncacheWrite) {
      when(io.axi.w.fire) {
        wvalid := false.B
      }
      when(io.axi.aw.fire) {
        awvalid := false.B
      }
      when(io.axi.b.fire) {
        imm_ansvalid   := true.B
        imm_cached_ans := 0.U // stand for it is no mean
        state          := idle
      }
    }

    is(checkdirty) {
      // when the line is dirty, send to writebuffer
      val lru_way = lru(savedInfo.index)
      val isDirty = dirty(savedInfo.index)(lru_way)
      when(isDirty) {
        state := writeBack0
      }.otherwise {
        arvalid  := true.B
        ar.addr  := savedInfo.addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
        ar.size  := 2.U
        ar.len   := (BANK_WIDTH / 4 - 1).U
        rready   := false.B
        linedata := 0.U
        wmask    := 1.U
        state    := replaceLine
      } // is not dirty
    }

    // TODO: mask merge
    is(writeBack0) {
      awvalid := true.B
      aw.addr := savedInfo.linetag(lru(savedInfo.index)) ## savedInfo.index ## 0.U(4.W)
      aw.size := 2.U
      aw.len  := (BANK_WIDTH / 4 - 1).U

      wvalid := true.B
      w.data := w_data(31, 0)
      w.strb := "b1111".U
      w.last := false.B
      count  := 1.U
      state  := writeBack1
    }

    is(writeBack1) {
      when(io.axi.aw.fire) {
        awvalid := false.B
      }
      when(io.axi.w.fire) {
        when(w.last) {
          wvalid := false.B
        }.otherwise {
          count := count << 1.U
          w.data := Mux1H(
            Seq(
              count(0) -> w_data(63, 32),
              count(1) -> w_data(95, 64),
              count(2) -> w_data(127, 96),
            ),
          )
          w.strb := "b1111".U
          w.last := count(2).asBool
        }
      }
      when(io.axi.b.fire) {
        arvalid  := true.B
        ar.addr  := savedInfo.addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
        ar.size  := 2.U
        ar.len   := (BANK_WIDTH / 4 - 1).U
        rready   := false.B
        linedata := 0.U
        wmask    := 1.U
        state    := replaceLine
      }
    }

    is(replaceLine) {
      val lru_way         = lru(savedInfo.index)
      val replaceComplete = RegInit(false.B)
      val writeComplete   = RegInit(false.B)
      when(io.axi.ar.valid) {
        when(io.axi.ar.ready) {
          arvalid := false.B
          rready  := true.B
        }
      }.elsewhen(io.axi.r.fire) {
        linedata := linedata | (io.axi.r.bits.data << wmove)
        wmask    := wmask << 1.U
        when(io.axi.r.bits.last) {
          rready          := false.B
          wmask           := 1.U
          replaceComplete := true.B
        }
      }

      when(replaceComplete) {
        for (i <- 0 until WAY_WIDTH) {
          data(i).addrb := savedInfo.addr(11, 4)
          tagV(i).addrb := savedInfo.addr(11, 4)
        }

        data(lru_way).wea               := true.B
        data(lru_way).addra             := savedInfo.index
        data(lru_way).dina              := linedata
        tagV(lru_way).wea               := true.B
        tagV(lru_way).addra             := savedInfo.index
        tagV(lru_way).dina              := savedInfo.tag ## 1.U(1.W)
        dirty(savedInfo.index)(lru_way) := false.B
        replaceComplete                 := false.B
        io.mem2.prevAwake               := true.B

        writeComplete := true.B
      }

      when(writeComplete) {
        state         := idle
        writeComplete := false.B
      }
    }
  }

  ar.id := 1.U
  // ar.len   := 0.U
  ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  aw.id := 1.U
  // aw.len   := 0.U
  aw.size  := 2.U
  aw.burst := 1.U
  aw.lock  := 0.U
  aw.cache := 0.U
  aw.prot  := 0.U

  w.id := 1.U
  // w.last := true.B

  io.axi.ar.bits        := ar
  io.axi.ar.valid       := arvalid
  io.axi.aw.bits        := aw
  io.axi.aw.valid       := awvalid
  io.axi.w.bits         := w
  io.axi.w.valid        := wvalid
  io.axi.r.ready        := rready
  io.axi.b.ready        := bready
  io.mem2.answer.valid  := imm_ansvalid
  io.mem2.answer.bits   := imm_cached_ans
  io.mem2.request.ready := true.B

  if (Config.debug_on) {
    dontTouch(hitdata)
  }
}