package memory.cache

import chisel3._
import chisel3.util._

import bundles._
import func.Functions._
import const.cacheConst._
import const.Parameters._
import const.Config

import pipeline._

class dCache_with_cached_writebufferIO extends Bundle {
  val axi  = new DCacheAXI
  val mem0 = Flipped(new Mem0DCacheIO)
  val mem1 = Flipped(new Mem1DCacheIO)
  // val mem2 = Flipped(new Mem2DCacheIO)
}

class dCache_with_cached_writebuffer extends Module {
  val io = IO(new dCache_with_cached_writebufferIO)
  io.mem1.answer_imm := false.B

  val w_idle :: w_work :: Nil                                                  = Enum(2)
  val idle :: replace_checkdirty :: replace :: uncached_r :: uncached_w :: Nil = Enum(5)

  val datasram =
    VecInit.fill(2)(Module(new xilinx_simple_dual_port_1_clock_ram_write_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val tagsram  = VecInit.fill(2)(Module(new xilinx_simple_dual_port_1_clock_ram_write_first(TAG_WIDTH, LINE_WIDTH)).io)
  val validreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(2)(false.B)))))
  val dirtyreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(2)(false.B)))))
  val write_buffer      = Module(new Queue(new Line(), 4)).io
  val write_buffer_addr = WireDefault(VecInit(Seq.fill(4)(0.U(ADDR_WIDTH.W))))
  val lru               = RegInit(VecInit(Seq.fill(LINE_WIDTH)(false.B)))

  val ar      = RegInit(0.U.asTypeOf(new AR))
  val arvalid = RegInit(false.B)
  val rready  = RegInit(false.B)
  val aw      = RegInit(0.U.asTypeOf(new AW))
  val awvalid = RegInit(false.B)
  val w       = RegInit(0.U.asTypeOf(new W))
  val wvalid  = RegInit(false.B)
  val bready  = true.B

  val axi_idle     = WireDefault(false.B)
  val ans_valid    = RegInit(false.B)
  val cached_ans   = RegInit(0.U(DATA_WIDTH.W))
  val uncached_ans = RegInit(0.U(DATA_WIDTH.W))

  val i_ans_valid  = WireDefault(false.B)
  val i_cached_ans = WireDefault(0.U(DATA_WIDTH.W))

  val saved_info     = RegInit(0.U.asTypeOf(new savedInfo))
  val state          = RegInit(idle)
  val wstate         = RegInit(w_idle)
  val cached         = WireDefault(true.B)
  val cacheBusy      = WireDefault(false.B)
  val total_requests = RegInit(0.U(32.W))
  val hitted_times   = RegInit(0.U(32.W))
  val hit            = Wire(Vec(2, Bool()))
  val hitted         = WireDefault(false.B)
  val hittedway      = WireDefault(false.B)
  val hitdataline    = WireDefault(0.U((LINE_SIZE * 8).W))
  val hitdata        = WireDefault(0.U(DATA_WIDTH.W))

  val is_uncached = RegInit(false.B)
  val linedata    = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask       = RegInit(1.U(4.W))
  val wmove       = Mux1H((3 to 0 by -1).map(i => wmask(i) -> (i * 32).U))
  val addr        = RegEnable(io.mem0.request.bits.addr, 0.U, io.mem0.request.valid)

  write_buffer.enq.valid := false.B
  write_buffer.deq.ready := false.B
  write_buffer.enq.bits  := 0.U.asTypeOf(new Line())
  val bufferFull  = !write_buffer.enq.ready
  val bufferEmpty = !write_buffer.deq.valid

  for (i <- 0 until 2) {
    datasram(i).clka  := clock
    datasram(i).addra := 0.U // used to write
    datasram(i).addrb := Mux(io.mem0.request.valid, io.mem0.request.bits.addr, addr)(11, 4)
    datasram(i).wea   := false.B
    datasram(i).dina  := 0.U
    tagsram(i).clka   := clock
    tagsram(i).addra  := 0.U // used to write
    tagsram(i).addrb  := Mux(io.mem0.request.valid, io.mem0.request.bits.addr, addr)(11, 4)
    tagsram(i).wea    := false.B
    tagsram(i).dina   := 0.U
  }

  hit         := VecInit.tabulate(2)(i => tagsram(i).doutb === addr(31, 12) && validreg(addr(11, 4))(i))
  hittedway   := hit(1)
  hitted      := hit.reduce(_ || _)
  hitdataline := Mux1H(hit, VecInit.tabulate(2)(i => datasram(i).doutb))
  hitdata := MateDefault(
    addr(3, 2),
    0.U,
    Seq(
      0.U -> hitdataline(31, 0),
      1.U -> hitdataline(63, 32),
      2.U -> hitdataline(95, 64),
      3.U -> hitdataline(127, 96),
    ),
  )

  // mem send query constantly, but we only response at once

  cached    := io.mem1.request.bits.cached
  cacheBusy := state =/= idle
  switch(state) {
    is(idle) {
      ans_valid := false.B
      // checkhit
      // this "!ans_valid" has bugs
      when(io.mem1.request.fire && !ans_valid) {
        when(cached) {
          if (Config.statistic_on) {
            total_requests := total_requests + 1.U
          }
          when(hitted) {
            if (Config.statistic_on) {
              hitted_times := hitted_times + 1.U
            }
            // io.mem.answer_imm := true.B
            lru(io.mem1.request.bits.addr(11, 4)) := !hittedway
            when(io.mem1.request.bits.we) {
              /*
                if only has one port
                if hit and write sram,
                should wait 2 cycles,
                the first used to write,
                the second used to receive addr
               */

              /*
                if has two ports,
                one to read,
                one to write
                then we can write directly
               */
              // if next_addr == this_write_addr don't worry
              // write to sram

              // when data hit in buffer

              // else
              datasram(hittedway).wea   := true.B
              datasram(hittedway).addra := io.mem1.request.bits.addr(11, 4) // use the write port
              datasram(hittedway).dina := Merge(
                io.mem1.request.bits.strb,
                hitdataline,
                io.mem1.request.bits.data,
                io.mem1.request.bits.addr(3, 2),
              )
              dirtyreg(io.mem1.request.bits.addr(11, 4))(hittedway) := true.B
            }.elsewhen(io.mem1.request.bits.re) {
              // when read hit, send the data to cpu
              // cached_ans   := hitdata
              i_cached_ans := hitdata
            }
            i_ans_valid := true.B
            // ans_valid   := true.B
            // is_uncached := false.B
          }.otherwise {
            // miss the hit, should work at write_buffer idle
            // save the info that used later
            saved_info.linedata := Seq(datasram(0).doutb, datasram(1).doutb)
            saved_info.op       := io.mem1.request.bits.we
            saved_info.addr     := io.mem1.request.bits.addr
            saved_info.linetag  := Seq(tagsram(0).doutb, tagsram(1).doutb)
            saved_info.wstrb    := io.mem1.request.bits.strb
            saved_info.wdata    := io.mem1.request.bits.data
            state               := replace_checkdirty
          }
        }.otherwise {
          // uncached
          when(io.mem1.request.bits.re) {
            arvalid := true.B
            rready  := false.B
            ar.addr := io.mem1.request.bits.addr
            ar.len  := 0.U
            state   := uncached_r
          }.elsewhen(io.mem1.request.bits.we && wstate === w_idle) {
            awvalid := true.B
            aw.addr := io.mem1.request.bits.addr
            aw.len  := 0.U
            wvalid  := true.B
            w.strb  := io.mem1.request.bits.strb
            w.data  := io.mem1.request.bits.data
            w.last  := true.B
            state   := uncached_w
          }
        }
      }.otherwise {
        axi_idle := true.B
      }
    }
    is(uncached_r) {
      when(io.axi.ar.valid) {
        when(io.axi.ar.ready) {
          arvalid := false.B
          rready  := true.B
        }
      }.elsewhen(io.axi.r.fire) {
        is_uncached  := true.B
        ans_valid    := true.B
        uncached_ans := io.axi.r.bits.data
        state        := idle
      }
    }
    is(uncached_w) {
      when(io.axi.w.fire) {
        wvalid := false.B
      }
      when(io.axi.aw.fire) {
        awvalid := false.B
      }
      when(io.axi.b.fire) {
        is_uncached  := true.B
        ans_valid    := true.B
        uncached_ans := 0.U // stand for it is no mean
        state        := idle
      }
    }
    is(replace_checkdirty) {
      // when the line is dirty, send to writebuffer
      val lru_way     = lru(saved_info.index)
      val can_replace = WireDefault(false.B)
      when(dirtyreg(saved_info.index)(lru_way)) {
        // when not full, put in it
        // when full, wait, send message to cpu that is busy
        when(!bufferFull) {
          // check if has any the same addr in the write_buffer
          when(write_buffer_addr.contains(saved_info.linetag(lru_way) ## saved_info.index ## 0.U(4.W))) {
            // if have, update it
            // how to update it ???
          }.otherwise {
            // if not, put in
            write_buffer.enq.valid := true.B
            // generate the true value
            write_buffer.enq.bits.data  := saved_info.linedata(lru_way)
            write_buffer.enq.bits.addr  := saved_info.linetag(lru_way) ## saved_info.index ## 0.U(4.W)
            write_buffer.enq.bits.valid := true.B
          }
          dirtyreg(saved_info.index)(lru_way) := false.B
          can_replace                         := true.B
        }
        // if full, just wait
      }.elsewhen(wstate =/= w_work || aw.addr =/= saved_info.addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)) {
        can_replace := true.B
      } // is not dirty

      when(can_replace) {
        // act with axi
        arvalid  := true.B
        ar.addr  := saved_info.addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
        ar.size  := 2.U
        ar.len   := (BANK_WIDTH / 4 - 1).U
        rready   := false.B
        linedata := 0.U
        wmask    := 1.U
        state    := replace
      }
    }
    is(replace) {
      // get linedata and replace
      val getline_complete = WireDefault(false.B)
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

      val final_linedata = linedata | (io.axi.r.bits.data << wmove)
      dontTouch(final_linedata)
      // write sram
      // when operation is write, then merge it
      val lru_way = lru(saved_info.index)
      val wdata = Mux(
        saved_info.op,
        Merge(
          saved_info.wstrb,
          final_linedata,
          saved_info.wdata,
          saved_info.offset,
        ),
        final_linedata,
      )
      when(getline_complete) {
        // write to sram
        datasram(lru_way).wea               := true.B
        datasram(lru_way).addra             := saved_info.index // use the write port
        datasram(lru_way).dina              := wdata
        tagsram(lru_way).wea                := true.B
        tagsram(lru_way).addra              := saved_info.index // use the write port
        tagsram(lru_way).dina               := saved_info.tag
        validreg(saved_info.index)(lru_way) := true.B
        dirtyreg(saved_info.index)(lru_way) := saved_info.op

        ans_valid   := true.B
        is_uncached := false.B
        // when operation == read, write directly and send answer to cpu(不复用idle)
        when(!saved_info.op) {
          cached_ans := MateDefault(
            saved_info.offset,
            0.U,
            Seq(
              0.U -> final_linedata(31, 0),
              1.U -> final_linedata(63, 32),
              2.U -> final_linedata(95, 64),
              3.U -> final_linedata(127, 96),
            ),
          )
        }
        state := idle
      }
    }
  }

  val wsaved_data  = RegInit(0.U(128.W))
  val wbuffer_mask = RegInit(1.U(4.W))
  // if axi and dcache is idle, then send the write_buffer to it

  switch(wstate) {
    is(w_idle) {
      when(axi_idle && !bufferEmpty && state === idle) {
        // pop the write_buffer
        write_buffer.deq.ready := true.B
        wsaved_data            := write_buffer.deq.bits.data

        // act with axi
        awvalid := true.B
        aw.addr := write_buffer.deq.bits.addr
        aw.size := 2.U
        aw.len  := (BANK_WIDTH / 4 - 1).U

        wvalid       := true.B
        w.data       := write_buffer.deq.bits.data(31, 0)
        w.strb       := "b1111".U
        w.last       := false.B
        wstate       := w_work
        wbuffer_mask := 1.U
      }
    }
    is(w_work) {
      when(io.axi.aw.fire) {
        awvalid := false.B
      }
      when(io.axi.w.fire) {
        when(w.last) {
          wvalid := false.B
        }.otherwise {
          wbuffer_mask := wbuffer_mask << 1.U
          w.data := Mux1H(
            Seq(
              wbuffer_mask(0) -> wsaved_data(63, 32),
              wbuffer_mask(1) -> wsaved_data(95, 64),
              wbuffer_mask(2) -> wsaved_data(127, 96),
            ),
          )
          w.strb := "b1111".U
          w.last := wbuffer_mask(2).asBool
        }
      }
      when(io.axi.b.fire) {
        wbuffer_mask := 1.U
        wstate       := w_idle
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
  io.mem1.answer.valid  := ans_valid || i_ans_valid
  io.mem1.answer.bits   := Mux(i_ans_valid, i_cached_ans, Mux(is_uncached, uncached_ans, cached_ans))
  io.mem1.request.ready := true.B
  io.mem0.request.ready := true.B

  if (Config.debug_on) {
    dontTouch(axi_idle)
    dontTouch(bufferFull)
    dontTouch(bufferEmpty)
  }

  if (Config.statistic_on) {
    dontTouch(total_requests)
    dontTouch(hitted_times)
  }
}