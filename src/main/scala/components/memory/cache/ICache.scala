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
}

class ICache extends Module {
  val io = IO(new ICacheIO)

  val idle :: replace :: waiting :: Nil = Enum(3)

  val datasram = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first((LINE_SIZE * 8), LINE_WIDTH)).io)
  val tagsram  = VecInit.fill(WAY_WIDTH)(Module(new xilinx_single_port_ram_read_first(TAG_WIDTH, LINE_WIDTH)).io)
  val validreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))
  val lru      = RegInit(VecInit(Seq.fill(LINE_WIDTH)(false.B)))

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
  val next_addr      = io.preFetch.request.bits.addr
  val addr           = RegInit(0.U(ADDR_WIDTH.W))
  addr := next_addr

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

  hit         := VecInit.tabulate(2)(i => tagsram(i).douta === addr(31, 12) && validreg(addr(11, 4))(i))
  hittedway   := PriorityEncoder(hit)
  hitted      := hit.reduce(_ || _)
  hitdataline := Mux1H(hit, VecInit.tabulate(2)(i => datasram(i).douta))

  val hitdatalineVec = VecInit.tabulate(4)(i => hitdataline((i + 1) * 32 - 1, i * 32))

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      ans_valid := false.B
      when(io.fetch.request.fire) {
        if (Config.statistic_on) {
          total_requests := total_requests + 1.U
        }
        when(hitted) {
          if (Config.statistic_on) {
            hitted_times := hitted_times + 1.U
          }
          val lru_index = io.fetch.request.bits(11, 4)
          lru(lru_index) := !hitdataline

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
        }
      }
    }
    is(replace) {
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
      val _ans_bits      = VecInit.tabulate(4)(i => final_linedata((i + 1) * 32 - 1, i * 32))

      // write sram
      val lru_way = lru(saved_info.index)
      when(getline_complete) {
        // write to sram
        datasram(lru_way).wea               := true.B
        datasram(lru_way).addra             := saved_info.index
        datasram(lru_way).dina              := final_linedata
        tagsram(lru_way).wea                := true.B
        tagsram(lru_way).addra              := saved_info.index
        tagsram(lru_way).dina               := saved_info.tag
        validreg(saved_info.index)(lru_way) := true.B
        state                               := Mux(io.fetch.cango, idle, waiting)
        ans_valid                           := true.B
        ans_bits                            := _ans_bits
        saved_ans_bits                      := _ans_bits
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
    dontTouch(hitted)
    dontTouch(hittedway)
    dontTouch(hitdataline)
    dontTouch(hitdatalineVec)
  }

  if (Config.statistic_on) {
    dontTouch(total_requests)
    dontTouch(hitted_times)
  }
}
