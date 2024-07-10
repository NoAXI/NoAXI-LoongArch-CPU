package memory.cache

import chisel3._
import chisel3.util._

import bundles._
import func.Functions._
import const.cacheConst._
import const.Parameters._
import const.Config

import pipeline._

class DCacheIO extends Bundle {
  val axi      = new DCacheAXI
  val mem0     = Flipped(new Mem0DCacheIO)
  val mem1     = Flipped(new Mem1DCacheIO)
  val mem2     = Flipped(new Mem2DCacheIO)
  val wbBuffer = Flipped(DecoupledIO(new BufferInfo))
}

class DCache extends Module {
  val io = IO(new DCacheIO)

  val tagV =
    VecInit.fill(WAY_WIDTH)(Module(new xilinx_simple_dual_port_1_clock_ram_write_first((TAG_WIDTH + 1), LINE_WIDTH)).io)
  val tag   = VecInit.tabulate(WAY_WIDTH)(i => tagV(i).doutb(TAG_WIDTH, 1))
  val valid = VecInit.tabulate(WAY_WIDTH)(i => tagV(i).doutb(0))
  val data =
    VecInit.fill(WAY_WIDTH)(Module(new xilinx_simple_dual_port_1_clock_ram_write_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val dirty       = RegInit(VecInit.fill(WAY_WIDTH)(VecInit.fill(LINE_WIDTH)(false.B)))
  val lru         = RegInit(VecInit.fill(LINE_WIDTH)(0.U(log2Ceil(WAY_WIDTH).W)))
  val storeBuffer = Module(new Queue(new StoreInfo, BUFFER_WIDTH))

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

  // mem 1: check hit & request
  val paddr           = io.mem1.request.bits.addr
  val cached          = io.mem1.request.bits.cached
  val rwType          = io.mem1.request.bits.rw
  val cacheHitVec     = VecInit.tabulate(WAY_WIDTH)(i => tag(i) === paddr(31, 12) && valid(i))
  val cacheHit        = cacheHitVec.reduce(_ || _)
  val cacheHitWay     = PriorityEncoder(cacheHitVec)
  val uncachedAns     = RegInit(0.U(DATA_WIDTH.W))
  val savedInfo       = RegInit(0.U.asTypeOf(new savedInfo))
  val count           = RegInit(1.U(4.W))
  val linedata        = RegInit(0.U((LINE_SIZE * 8).W))
  val wmask           = RegInit(1.U(4.W))
  val wmove           = Mux1H((3 to 0 by -1).map(i => wmask(i) -> (i * 32).U))
  val afterReplace    = WireDefault(false.B)
  val replaceDataLine = WireDefault(0.U((LINE_SIZE * 8).W))

  for (i <- 0 until WAY_WIDTH)
    data(i).addrb := paddr(11, 4)

  val idle :: uncacheRead :: writeBack :: replaceLine :: Nil = Enum(4)
  val state                                                  = RegInit(idle)

  switch(state) {
    is(idle) {
      when(io.mem1.request.fire && !io.mem1.request.bits.rw) {
        when(cached) {
          when(cacheHit) {
            io.mem1.answer.valid := true.B
          }.otherwise {
            savedInfo.addr     := paddr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
            savedInfo.linedata := Seq(data(0).doutb, data(1).doutb)
            state              := Mux(dirty(cacheHitWay)(paddr(11, 4)), writeBack, replaceLine)
          }
        }.otherwise {
          state := uncacheRead
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
        state                := idle
        io.mem1.answer.valid := true.B
        uncachedAns          := io.axi.r.bits.data
      }
    }

    is(writeBack) {
      val wdata = savedInfo.linedata(lru(savedInfo.index))

      val _aw :: _w :: Nil = Enum(2)
      val wstate           = RegInit(_aw)
      switch(wstate) {
        is(_aw) {
          awvalid := true.B
          aw.addr := savedInfo.addr
          aw.size := 2.U
          aw.len  := (BANK_WIDTH / 4 - 1).U

          wvalid := true.B
          w.data := wdata(31, 0)
          w.strb := "b1111".U
          w.last := false.B
          count  := 1.U
          wstate := _w
        }
        is(_w) {
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
                  count(0) -> wdata(63, 32),
                  count(1) -> wdata(95, 64),
                  count(2) -> wdata(127, 96),
                ),
              )
              w.strb := "b1111".U
              w.last := count(2).asBool
            }
          }
          when(io.axi.b.fire) {
            linedata := 0.U
            wmask    := 1.U
            state    := replaceLine
            wstate   := _aw
          }
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

        afterReplace         := true.B
        replaceDataLine      := linedata
        io.mem1.answer.valid := true.B

        data(lru_way).wea               := true.B
        data(lru_way).addra             := savedInfo.index
        data(lru_way).dina              := linedata
        tagV(lru_way).wea               := true.B
        tagV(lru_way).addra             := savedInfo.index
        tagV(lru_way).dina              := savedInfo.tag ## 1.U(1.W)
        dirty(savedInfo.index)(lru_way) := false.B
        replaceComplete                 := false.B
      }
    }
  }

  // mem 2: get res
  val hitdataline = Mux(
    ShiftRegister(afterReplace, 1),
    ShiftRegister(replaceDataLine, 1),
    Mux1H(ShiftRegister(cacheHitVec, 1), VecInit.tabulate(2)(i => data(i).doutb)),
  )
  val hitdata = MateDefault(
    ShiftRegister(paddr(3, 2), 1),
    0.U,
    Seq(
      0.U -> hitdataline(31, 0),
      1.U -> hitdataline(63, 32),
      2.U -> hitdataline(95, 64),
      3.U -> hitdataline(127, 96),
    ),
  )
  val isUncached = ShiftRegister(!cached, 1)
  io.mem2.data := Mux(isUncached, uncachedAns, hitdata)

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

  io.axi.ar.bits  := ar
  io.axi.ar.valid := arvalid
  io.axi.aw.bits  := aw
  io.axi.aw.valid := awvalid
  io.axi.w.bits   := w
  io.axi.w.valid  := wvalid
  io.axi.r.ready  := rready
  io.axi.b.ready  := bready
}
