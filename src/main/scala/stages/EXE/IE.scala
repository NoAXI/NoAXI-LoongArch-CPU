package stages

import chisel3._
import chisel3.util._

import config._
import config.Functions._

class IE_IO extends Bundle with Parameters {
    val from = Flipped(DecoupledIO(new info))
    val to = DecoupledIO(new info)

    //** to sram
    // val data_sram_en = Output(Bool())
    // val data_sram_we = Output(UInt(INST_WIDTH_B.W))
    // val data_sram_addr = Output(UInt(ADDR_WIDTH.W)) 
    // val data_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IE extends Module with Parameters {
    val io = IO(new IE_IO)

    //与上一流水级握手，获取上一流水级信息
    val info = ConnectGetBus(io.from, io.to)
    
    val alu_op             = info.alu_op
    val es_load_op         = info.load_op
    val src1_is_pc         = info.src1_is_pc.asBool
    val src2_is_imm        = info.src2_is_imm.asBool
    val src2_is_4          = info.src2_is_4.asBool
    val gr_we              = info.gr_we
    val es_mem_we          = info.mem_we.asBool
    val dest               = info.dest
    val imm                = info.imm
    val rj_value           = info.rj_value
    val rkd_value          = info.rkd_value
    val es_pc              = info.pc
    val res_from_mem       = info.res_from_mem

    // did't use in lab7
    val es_res_from_mem = es_load_op

    // 调用ALU获得运算结果
    val alu = Module(new ALU)
    alu.io.alu_op := alu_op
    alu.io.alu_src1 := Mux(src1_is_pc, es_pc, rj_value)
    alu.io.alu_src2 := Mux(src2_is_imm, imm, Mux(src2_is_4, 4.U, rkd_value))
    val alu_result = alu.io.alu_result

    val to_info = WireDefault(0.U.asTypeOf(new info))
    to_info.res_from_mem := res_from_mem
    to_info.gr_we := gr_we
    to_info.dest := dest
    to_info.alu_result := alu_result
    to_info.pc := es_pc
    to_info.mem_we := es_mem_we
    to_info.rkd_value := rkd_value
    io.to.bits := to_info

    // io.data_sram_en := true.B
    // io.data_sram_we := Mux(es_mem_we && es_valid, 4.U, 0.U)
    // // io.data_sram_addr := alu_result
    // io.data_sram_waddr := alu_result
    // io.data_sram_wdata := rkd_value
}