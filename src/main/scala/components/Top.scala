package components

import chisel3._
import chisel3.util._

import stages._
import bundles._
import config._
import config.Functions._

class TopIO extends Bundle {
    val tmp = Input(UInt(32.W))
}

class Top extends Module {
    val io = IO(new TopIO())

    val alu = Module(new ALU())
    val grreg = Module(new GRReg())

    // 连线暂略，bundle未调整好
}

// object main extends App {
//     emitVerilog(new IW(), Array("--target-dir", "wave"))
//     println("ok!")
// }
