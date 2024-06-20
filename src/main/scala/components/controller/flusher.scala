package controller

import chisel3._
import chisel3.util._

// function: flush all the stages before the apply stage
// make sure that only one application can be received

class FlusherIO extends Bundle {
  val flush = Output(Vec(5, Bool()))
  val apply = Input(Vec(5, Bool()))
}

class Flusher extends Module {
  val io = IO(new FlusherIO)

  for(i <- 0 until 5) {
    io.flush(i) := false.B
  }

  for(i <- 0 until 5) {
    when(io.apply(i)) {
      for(j <- 0 until i) {
        io.flush(j) := true.B
      }
    }
  }
}
