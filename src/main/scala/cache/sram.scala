package cache

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import Funcs.Functions._

class Sram extends Module {
  val io  = IO(Flipped(new data_sramIO))
  val mem = SyncReadMem(1024, Vec(16, UInt(8.W)))

  val wmask = VecInit(
    (0 until 4).map(i => io.we(i).asBool),
  )

  val wdata = VecInit(
    (0 until 4).map(i => io.wdata((i + 1) * 8 - 1, i * 8)),
  )

  mem.write(io.addr, wdata, wmask)
  io.rdata := mem.read(io.addr, io.en)
}
