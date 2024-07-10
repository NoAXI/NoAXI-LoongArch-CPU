package memory.cache

import chisel3._
import chisel3.util._

import bundles._
import func.Functions._
import const.cacheConst._
import const.Parameters._
import const.Config

class StoreBufferIO extends Bundle {
  val in = Flipped(Decoupled(new savedInfo()))
  val out = Decoupled(new savedInfo())
}

