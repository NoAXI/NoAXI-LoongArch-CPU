package controller

import chisel3._
import chisel3.util._

import bundles._
import const.Parameters._
import configs.CpuConfig

trait ForwarderConst {
  val FORWARD_REG_NUM = 3
  val FORWARD_NUM     = 3
  val FORWARD_EXE_NUM = 2
  val FORWARD_MEM_NUM = 1
  val FORWARD_WB_NUM  = 0
}

class ForwarderIO extends Bundle with ForwarderConst {
  val load_complete = Input(Bool())                            // mem -> forward
  val dataIn        = Input(Vec(FORWARD_NUM, new ForwardData)) // (exe, mem, wb) -> forward
  val forward_query = Input(new ForwardQuery)                  // decode -> forward
  val forward_ans   = Output(new ForwardAns)                   // forward -> decode
}

// to do：前递两个够用了，rj rkd，现在这样做很可能会存在无用的等
class Forwarder extends Module with ForwarderConst {
  val io = IO(new ForwarderIO)

  // regfile forward & load hit check
  val exe_load_hit = WireDefault(false.B)
  val mem_load_hit = WireDefault(false.B)
  for (i <- 0 until FORWARD_REG_NUM) {
    val addr = io.forward_query.addr(i)
    io.forward_ans.data(i) := io.forward_query.ini_data(i)
    for (j <- 0 until FORWARD_NUM) {
      // assume that the program won't use reg_0
      when(addr === io.dataIn(j).addr && io.dataIn(j).we && addr =/= 0.U) {
        io.forward_ans.data(i) := io.dataIn(j).data
        when(io.dataIn(j).isld) {
          if (j == FORWARD_EXE_NUM) {
            exe_load_hit := true.B
          } else if (j == FORWARD_MEM_NUM) {
            mem_load_hit := true.B
          }
        }
      }
    }
  }

  // csr forward
  // if the write csr operation has mask??
  val csr_addr = io.forward_query.csr_addr
  io.forward_ans.csr_data := WireDefault(io.forward_query.csr_ini_data)
  for (j <- 0 until FORWARD_NUM) {
    when(csr_addr === io.dataIn(j).csr_addr && io.dataIn(j).csr_we) {
      io.forward_ans.csr_data := io.dataIn(j).csr_data
    }
  }

  // load stall signal -> decode
  io.forward_ans.notld := WireDefault(false.B)
  when(exe_load_hit) {
    io.forward_ans.notld := true.B
  }.elsewhen(mem_load_hit) {
    io.forward_ans.notld := !io.load_complete
  }

  if (CpuConfig.debug_on) {
    dontTouch(io.dataIn)
    dontTouch(exe_load_hit)
    dontTouch(mem_load_hit)
  }
}
