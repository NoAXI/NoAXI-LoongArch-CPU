package bundles

import chisel3._
import chisel3.util._
import const.Parameters._
import const.Predict._

class PreFetchBPUIO extends Bundle {
  val stall    = Output(Bool())
  val pcValid  = Output(Vec(FETCH_DEPTH, Bool()))
  val pcGroup  = Output(Vec(FETCH_DEPTH, UInt(ADDR_WIDTH.W)))
  val npcGroup = Output(Vec(FETCH_DEPTH, UInt(ADDR_WIDTH.W)))
  val train    = Output(new PredictRes)
  val nextPC   = Input(new br)
  // val predict  = Input(Vec(FETCH_DEPTH, new br))
}

// 不是跳转指令br.en = false.B
class PredictRes extends Bundle {
  val isbr          = Bool() // is branch
  val br            = new br // is predict fail
  val realDirection = Bool() // branch direction
  val pc            = UInt(ADDR_WIDTH.W)
//   val isCALL        = Bool() // is call or PC-relative branch
//   val isReturn      = Bool() // is Return

  def index = pc(INDEX_LENGTH + 3, 4)
//   def BTBIndex = pc(BTB_INDEX_LENGTH + 3, 4)
//   def BTBTag   = pc(ADDR_WIDTH - 1, ADDR_WIDTH - BTB_TAG_LENGTH)
}
