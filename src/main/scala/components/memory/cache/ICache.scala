package memory.cache

import chisel3._
import chisel3.util._

import const._
import bundles._
import const.cacheConst._
import const.Parameters._
import func.Functions._

class ICacheIO extends Bundle {
  val axi      = new ICacheAXI
  val preFetch = Flipped(new PreFetchICacheIO)
  val fetch    = Flipped(new FetchICacheIO)

  val cacop = Input(new CacOpInfo)

  val succeed_time = if (Config.statistic_on) Some(Output(UInt(DATA_WIDTH.W))) else None
  val total_time   = if (Config.statistic_on) Some(Output(UInt(DATA_WIDTH.W))) else None
}

class ICache extends Module {
  val io = IO(new ICacheIO)

  //   0        1           2             3
  val idle :: replace :: waiting :: uncacheRead :: Nil = Enum(4)

  val datasram = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val tagsram  = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first(TAG_WIDTH, LINE_WIDTH)).io)
  val validreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))
  val lru      = Module(new xilinx_single_port_ram_read_first(1, LINE_WIDTH)).io

  val ar             = RegInit(0.U.asTypeOf(new AR))
  val arvalid        = RegInit(false.B)
  val rready         = RegInit(false.B)
  val ans_valid      = RegInit(false.B)
  val ans_bits       = RegInit(VecInit.fill(4)(0.U(INST_WIDTH.W)))
  val i_ans_valid    = WireDefault(false.B)
  val i_ans_bits     = WireDefault(VecInit.fill(4)(0.U(INST_WIDTH.W)))
  val saved_ans_bits = RegInit(VecInit.fill(4)(0.U(INST_WIDTH.W)))
  val total_requests = RegInit(0.U(32.W))
  val hitted_times   = RegInit(0.U(32.W))
  val hit            = Wire(Vec(2, Bool()))
  val hitted         = WireDefault(false.B)
  val hittedway      = WireDefault(false.B)
  val hitdataline    = WireDefault(0.U((LINE_SIZE * 8).W))
  val linedata       = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask          = RegInit(1.U(4.W))
  val wmove          = Mux1H((3 to 0 by -1).map(i => wmask(i) -> (i * 32).U))
  val saved_info     = RegInit(0.U.asTypeOf(new savedInfo))
  val saved_cached   = RegInit(false.B)
  val next_addr      = io.preFetch.request.bits.addr
  val addr           = io.fetch.request.bits

  for (i <- 0 until 2) {
    datasram(i).clka  := clock
    datasram(i).addra := next_addr(11, 4)
    datasram(i).wea   := false.B
    datasram(i).dina  := 0.U
    tagsram(i).clka   := clock
    tagsram(i).addra  := next_addr(11, 4)
    tagsram(i).wea    := false.B
    tagsram(i).dina   := 0.U
  }

  val state = RegInit(idle)

  lru.clka  := clock
  lru.addra := Mux(state === idle, io.fetch.request.bits(11, 4), saved_info.index)
  lru.wea   := false.B
  lru.dina  := 0.U

  hit         := VecInit.tabulate(2)(i => tagsram(i).douta === addr(31, 12) && validreg(addr(11, 4))(i))
  hittedway   := PriorityEncoder(hit)
  hitted      := hit.reduce(_ || _)
  hitdataline := Mux1H(hit, VecInit.tabulate(2)(i => datasram(i).douta))

  val hitdatalineVec = VecInit.tabulate(4)(i => hitdataline((i + 1) * 32 - 1, i * 32))

  val lruway = RegNext(lru.douta)
  switch(state) {
    is(idle) {
      ans_valid := false.B
      when(io.fetch.request.fire) {
        when(io.fetch.cached || true.B) { // !!! TODO: uncached fetch logic !!!
          if (Config.statistic_on) {
            total_requests := total_requests + 1.U
          }
          when(hitted) {
            if (Config.statistic_on) {
              hitted_times := hitted_times + 1.U
            }
            lru.wea  := true.B
            lru.dina := !hittedway

            state          := Mux(io.fetch.cango, idle, waiting)
            i_ans_valid    := true.B
            i_ans_bits     := hitdatalineVec
            saved_ans_bits := hitdatalineVec
          }.otherwise {
            arvalid         := true.B
            ar.addr         := addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
            ar.size         := 2.U
            ar.len          := (BANK_WIDTH / 4 - 1).U
            rready          := false.B
            linedata        := 0.U
            wmask           := 1.U
            state           := replace
            saved_info.addr := addr
            saved_cached    := io.fetch.cached
          }
        }.otherwise {
          i_ans_valid := false.B
          arvalid     := true.B
          rready      := false.B
          ar.addr     := addr(ADDR_WIDTH - 1, 2) ## 0.U(2.W) // pay attention
          ar.len      := 0.U
          state       := uncacheRead
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
        i_ans_valid := true.B
        i_ans_bits  := VecInit(io.axi.r.bits.data, io.axi.r.bits.data, io.axi.r.bits.data, io.axi.r.bits.data)
        state       := idle
      }
    }

    is(replace) {
      val getline_complete = RegInit(false.B)
      when(io.axi.ar.valid) {
        when(io.axi.ar.ready) {
          arvalid := false.B
          rready  := true.B
        }
      }.elsewhen(io.axi.r.fire) {
        linedata := linedata | (io.axi.r.bits.data << wmove)
        wmask    := wmask << 1.U
        when(io.axi.r.bits.last) {
          rready           := false.B
          wmask            := 1.U
          getline_complete := true.B
        }
      }

      val _ans_bits = VecInit.tabulate(4)(i => linedata((i + 1) * 32 - 1, i * 32))

      // write sram
      when(getline_complete) {
        // write to sram
        getline_complete                   := false.B
        datasram(lruway).wea               := true.B
        datasram(lruway).addra             := saved_info.index
        datasram(lruway).dina              := linedata
        tagsram(lruway).wea                := true.B
        tagsram(lruway).addra              := saved_info.index
        tagsram(lruway).dina               := saved_info.tag
        validreg(saved_info.index)(lruway) := true.B && saved_cached
        state                              := Mux(io.fetch.cango, idle, waiting)
        ans_valid                          := true.B
        ans_bits                           := _ans_bits
        saved_ans_bits                     := _ans_bits
      }
    }
    is(waiting) {
      // to do:
      // when waiting for a long time, we can request the axi with the next pc
      // don't waste the time!
      ans_valid   := false.B
      i_ans_valid := true.B
      i_ans_bits  := saved_ans_bits
      state       := Mux(io.fetch.cango, idle, waiting)
    }
  }

  when(io.cacop.isICache && io.cacop.en) {
    switch(io.cacop.opType) {
      is(0.U) {
        // QUESTION: after the cacop inst that fetched, modify the sram, right??

        for (i <- 0 until WAY_WIDTH) {
          tagsram(i).wea   := true.B
          tagsram(i).addra := io.cacop.index
          tagsram(i).dina  := 0.U
        }
      }

      is(1.U) {
        for (i <- 0 until WAY_WIDTH) {
          validreg(io.cacop.index)(i) := false.B
        }
      }

      is(2.U) {
        for (i <- 0 until WAY_WIDTH) {
          validreg(io.cacop.index)(i) := false.B
        }
      }
    }
  }

  ar.id := 0.U
  // ar.len   := 0.U
  ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  io.axi.ar.bits            := ar
  io.axi.ar.valid           := arvalid
  io.axi.r.ready            := rready
  io.preFetch.request.ready := true.B
  io.fetch.answer.valid     := ans_valid || i_ans_valid
  io.fetch.answer.bits      := Mux(i_ans_valid, i_ans_bits, ans_bits)
  io.fetch.request.ready    := true.B

  if (Config.debug_on) {
    dontTouch(i_ans_valid)
    dontTouch(hitted)
    dontTouch(hittedway)
    dontTouch(hitdataline)
    dontTouch(hitdatalineVec)
  }

  if (Config.statistic_on) {
    dontTouch(total_requests)
    dontTouch(hitted_times)
    io.succeed_time.get := hitted_times
    io.total_time.get   := total_requests
  }
}
