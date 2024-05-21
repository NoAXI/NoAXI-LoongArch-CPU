package axi

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._

class iCacheIO extends Bundle {
  val axi   = new iCache_AXI
  val fetch = Flipped(new fetch_iCache_IO)
  val stall = Output(Bool())
}

class iCache extends Module {
  val io = IO(new iCacheIO)

  val ar          = RegInit(0.U.asTypeOf(new AR))
  val arvalid     = RegInit(false.B)
  val rready      = RegInit(false.B)
  val ans_valid   = RegInit(false.B)
  val ans_bits    = RegInit(0.U(INST_WIDTH.W))
  val req_ready   = RegInit(true.B)
  val stall       = RegInit(false.B)
  val saved_valid = RegInit(false.B)
  val saved_inst  = RegInit(0.U(INST_WIDTH.W))
  val saved_query = RegInit(false.B)
  val saved_addr  = RegInit(0.U(32.W))

  val idle :: state0 :: state1 :: waiting :: Nil = Enum(4)

  val state = RegInit(idle)
  switch(state) {
    is(idle) {
      ans_valid := false.B
      when(io.fetch.request.fire) {
        arvalid     := true.B
        ar.addr     := io.fetch.request.bits
        rready      := false.B
        state       := state0
      }.elsewhen(saved_query) {
        saved_query := false.B
        arvalid     := true.B
        ar.addr     := saved_addr
        rready      := false.B
        state       := state0
      }
    }
    is(state0) {
      stall     := true.B
      req_ready := false.B
      when(io.axi.ar.ready) {
        arvalid := false.B
        rready  := true.B
        state   := state1
      }
    }
    is(state1) {
      stall     := true.B
      req_ready := false.B
      when(io.axi.r.valid) {
        ans_valid := true.B
        ans_bits  := io.axi.r.bits.data
        when(io.fetch.cango) {
          stall     := false.B
          req_ready := true.B
          state     := idle
        }.otherwise {
          saved_inst  := io.axi.r.bits.data
          saved_valid := true.B
          state       := waiting
        }
      }
    }
    is(waiting) {
      // 如果在这个阶段收到请求，需要保存住
      when(io.fetch.request.valid) {
        saved_query := true.B
        saved_addr  := io.fetch.request.bits
      }
      ans_bits  := saved_inst
      ans_valid := saved_valid
      when(io.fetch.cango) {
        ans_valid := false.B
        req_ready := true.B
        state     := idle
      }
    }
  }

  ar.id    := DontCare
  ar.len   := 0.U
  ar.size  := 2.U
  ar.burst := 1.U
  ar.lock  := 0.U
  ar.cache := 0.U
  ar.prot  := 0.U

  io.axi.ar.bits         := ar
  io.axi.ar.valid        := arvalid
  io.axi.r.ready         := rready
  io.fetch.answer.valid  := ans_valid
  io.fetch.answer.bits   := ans_bits
  io.fetch.request.ready := req_ready
  io.stall               := stall
}
