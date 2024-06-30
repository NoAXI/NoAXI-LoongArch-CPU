package pipeline

import chisel3._
import chisel3.util._

import const.Parameters._
import bundles._

class ROBIO extends Bundle {
  val rename = new Bundle {
    val valid = Input(Vec(ISSUE_WIDTH, Bool()))
    val bits  = Input(Vec(ISSUE_WIDTH, new ROBInfo))
    val stall = Output(Bool())
  }
  // val commit = new Bundle {
  //   val 
  // }
}
class ROB extends Module {
  val io  = IO(new ROBIO)
  val rob = RegInit(VecInit(Seq.fill(ROB_NUM)(0.U.asTypeOf(new ROBInfo))))

}
