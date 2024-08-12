package memory.cache

import chisel3._
import chisel3.util._

import bundles._
import func.Functions._
import const.cacheConst._
import const.Parameters._
import const.Config

import pipeline._
import const.cacheConst

class DCacheIO extends Bundle {
  val axi = new DCacheAXI
  // val mem0 = Flipped(new Mem0DCacheIO)
  val mem1 = Flipped(new Mem1DCacheIO)
  val mem2 = Flipped(new Mem2DCacheIO)

  val succeed_time = if (Config.statistic_on) Some(Output(UInt(DATA_WIDTH.W))) else None
  val total_time   = if (Config.statistic_on) Some(Output(UInt(DATA_WIDTH.W))) else None
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

  val total_requests = RegInit(0.U(32.W))
  val hitted_times   = RegInit(0.U(32.W))

  // mem 1: send va & exception
  val vaddr = io.mem1.addr
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

  // mem 2: act with D-Cache
  //   0           1               2             3              4            5              6
  val idle :: uncacheRead :: uncacheWrite :: checkdirty :: writeBack0 :: writeBack1 :: replaceLine :: Nil = Enum(7)

  //   0         1             2          3
  val init :: firstWay :: secondWay :: readDirty :: Nil = Enum(4)

  val state  = RegInit(idle)
  val cached = io.mem2.request.bits.cached
  val pa     = io.mem2.request.bits.addr
  val wdata  = io.mem2.request.bits.wdata
  val wstrb  = io.mem2.request.bits.wstrb
  val rwType = io.mem2.rwType
  val hitVec = VecInit.tabulate(WAY_WIDTH)(i => tag(i) === pa(31, 12) && valid(i))

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

  val cacop         = io.mem2.request.bits.cacop
  val cacopen       = cacop.en && cacop.isDCache
  val cacop_flag    = RegInit(false.B)
  val not_complete  = RegInit(false.B)
  val imm_ansvalid  = WireDefault(false.B)
  val imm_ans       = WireDefault(0.U(DATA_WIDTH.W))
  val savedInfo     = RegInit(0.U.asTypeOf(new savedInfo))
  val count         = RegInit(1.U(3.W))
  val linedata      = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask         = RegInit(1.U(4.W))
  val wmove         = Mux1H(((DATA_WIDTH / 8 - 1) to 0 by -1).map(i => wmask(i) -> (i * 32).U))
  val w_data        = Mux(cacop_flag, savedInfo.linedata(0), savedInfo.linedata(lru(savedInfo.index)))
  val cacop_state   = RegInit(init)
  val saved_line    = RegInit(0.U((LINE_SIZE * 8).W))
  val saved_tag     = RegInit(0.U(TAG_WIDTH.W))
  val ibarLineIndex = RegInit(0.U(LINE_WIDTH_LOG.W))
  val dirtySignal   = RegInit(false.B)
  val dirtyRIndex   = RegInit(0.U(LINE_WIDTH_LOG.W))
  val dirtyRWay     = RegInit(false.B)

  switch(state) {
    is(idle) {
      imm_ansvalid := true.B
      when(io.mem2.request.fire) {
        when(cacopen) {
          switch(cacop.opType) {
            is(0.U) {
              for (i <- 0 until WAY_WIDTH) {
                tagV(i).wea   := true.B
                tagV(i).addra := cacop.index
                tagV(i).dina  := 0.U
              }
            }

            is(2.U) {
              when(cacheHit) {
                // QUESTION: why Invalidate ??
                imm_ansvalid := false.B
                // tagV(cacheHitWay).wea   := true.B
                // tagV(cacheHitWay).addra := cacop.index
                // tagV(cacheHitWay).dina  := 0.U

                // writeback
                dirty(cacop.index)(cacheHitWay) := false.B
                cacop_flag                      := true.B
                state                           := writeBack0
                savedInfo.linedata(0)           := hitdataline
                savedInfo.addr                  := tag(cacheHitWay) ## cacop.index ## 0.U(4.W)
                not_complete                    := false.B
              }
            }

            is(1.U) {
              imm_ansvalid := false.B

              switch(cacop_state) {
                is(init) {
                  for (i <- 0 until WAY_WIDTH) {
                    data(i).addrb := cacop.index
                    tagV(i).addrb := cacop.index
                  }
                  cacop_state := firstWay
                }
                is(firstWay) {
                  cacop_state := secondWay
                  when(dirty(cacop.index)(0)) {
                    dirty(cacop.index)(0) := false.B
                    cacop_flag            := true.B
                    state                 := writeBack0
                    savedInfo.linedata(0) := data(0).doutb
                    savedInfo.addr        := tag(0) ## cacop.index ## 0.U(4.W)
                    not_complete          := true.B
                  }
                  saved_line := data(1).doutb
                  saved_tag  := tag(1)
                }
                is(secondWay) {
                  cacop_state := init
                  when(dirty(cacop.index)(1)) {
                    dirty(cacop.index)(1) := false.B
                    cacop_flag            := true.B
                    state                 := writeBack0
                    savedInfo.linedata(0) := saved_line
                    savedInfo.addr        := saved_tag ## cacop.index ## 0.U(4.W)
                    not_complete          := false.B
                  }.otherwise {
                    imm_ansvalid := true.B
                  }
                }
              }
            }

            is(3.U) {
              imm_ansvalid := false.B

              switch(cacop_state) {
                is(init) {
                  for (i <- 0 until WAY_WIDTH) {
                    data(i).addrb := ibarLineIndex
                    tagV(i).addrb := ibarLineIndex
                  }
                  cacop_state := firstWay
                }
                is(firstWay) {
                  cacop_state := secondWay
                  when(dirty(ibarLineIndex)(0)) {
                    dirty(ibarLineIndex)(0) := false.B
                    cacop_flag              := true.B
                    state                   := writeBack0
                    savedInfo.linedata(0)   := data(0).doutb
                    savedInfo.addr          := tag(0) ## ibarLineIndex ## 0.U(4.W)
                    not_complete            := true.B
                  }
                  saved_line := data(1).doutb
                  saved_tag  := tag(1)
                }
                is(secondWay) {
                  cacop_state   := init
                  ibarLineIndex := ibarLineIndex + 1.U
                  when(dirty(ibarLineIndex)(1)) {
                    dirty(ibarLineIndex)(1) := false.B
                    cacop_flag              := true.B
                    state                   := writeBack0
                    savedInfo.linedata(0)   := saved_line
                    savedInfo.addr          := saved_tag ## ibarLineIndex ## 0.U(4.W)
                    not_complete            := ibarLineIndex =/= (LINE_WIDTH - 1).U
                  }.otherwise {
                    imm_ansvalid := ibarLineIndex === (LINE_WIDTH - 1).U
                  }
                }
              }
            }
          }
        }.elsewhen(cached) {
          if (Config.statistic_on) {
            total_requests := total_requests + 1.U
          }
          when(cacheHit) {
            if (Config.statistic_on) {
              hitted_times := hitted_times + 1.U
            }
            lru(pa(11, 4)) := !cacheHitWay
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
              imm_ans := hitdata
            }
          }.otherwise {
            imm_ansvalid       := false.B
            savedInfo.linedata := VecInit(data(0).doutb, data(1).doutb)
            savedInfo.op       := rwType
            savedInfo.addr     := pa
            savedInfo.linetag  := VecInit(tag(0), tag(1))
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
        imm_ansvalid := true.B
        imm_ans      := io.axi.r.bits.data
        state        := idle
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
        imm_ansvalid := true.B
        imm_ans      := 0.U // stand for it is no mean
        state        := idle
      }
    }

    is(checkdirty) {
      // when the line is dirty, send to writebuffer
      val isDirty = dirty(savedInfo.index)(lru(savedInfo.index))
      when(isDirty) {
        cacop_flag := false.B
        state      := writeBack0
      }.otherwise {
        arvalid    := true.B
        ar.addr    := savedInfo.addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
        ar.size    := 2.U
        ar.len     := (BANK_WIDTH / 4 - 1).U
        rready     := false.B
        linedata   := 0.U
        wmask      := 1.U
        cacop_flag := false.B
        state      := replaceLine
      } // is not dirty
    }

    is(writeBack0) {
      awvalid := true.B
      aw.addr := Mux(cacop_flag, savedInfo.addr, savedInfo.linetag(lru(savedInfo.index)) ## savedInfo.index ## 0.U(4.W))
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
        when(cacop_flag) {
          imm_ansvalid := !not_complete
          state        := idle
        }.otherwise {
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
    }

    is(replaceLine) {
      val lru_way         = lru(savedInfo.index)
      val replaceComplete = RegInit(false.B)
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
        state := idle

        for (i <- 0 until WAY_WIDTH) {
          data(i).addrb := savedInfo.index
          tagV(i).addrb := savedInfo.index
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
  io.mem2.answer.bits   := imm_ans
  io.mem2.request.ready := true.B

  if (Config.debug_on) {
    dontTouch(hitdata)
  }

  if (Config.statistic_on) {
    dontTouch(total_requests)
    dontTouch(hitted_times)
    io.succeed_time.get := hitted_times
    io.total_time.get   := total_requests
  }
}
