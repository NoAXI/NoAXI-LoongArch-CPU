package stages

import chisel3._
import chisel3.util._

import config._

class IE_IO extends Bundle with Parameters {
    //allow in
    val ms_allowin = Input(Bool())
    val es_allowin = Output(Bool())

    //from ds
    val ds_to_es_valid = Input(Bool()) 
    // val ds_to_es_bus = Input(UInt(DS_TO_ES_BUS_WIDTH.W)) 
    val ds_to_es_bus = Input(new ds_to_es_bus)

    //to ms
    val es_to_ms_valid = Output(Bool())
    // val es_to_ms_bus = Output(UInt(ES_TO_MS_BUS_WIDTH.W)) 
    val es_to_ms_bus = Output(new es_to_ms_bus) 


    // //** to sram
    // val data_sram_en = Output(Bool())
    // val data_sram_we = Output(UInt(INST_WIDTH_B.W))
    // // val data_sram_addr = Output(UInt(ADDR_WIDTH.W)) 
    // val data_sram_waddr = Output(UInt(ADDR_WIDTH.W)) //命名统一
    // val data_sram_wdata = Output(UInt(INST_WIDTH.W))
}

class IE extends Module with Parameters {
    val io = IO(new IE_IO)

    //与上一流水级握手，获取上一流水级信息
    // val ds_to_es_bus_r = RegInit(0.U(DS_TO_ES_BUS_WIDTH.W))
    val ds_to_es_bus_r = RegInit(0.U.asTypeOf(new ds_to_es_bus))
    val es_valid = RegInit(false.B)
    val es_ready_go = true.B
    io.es_allowin := !es_valid || es_ready_go && io.ms_allowin
    io.es_to_ms_valid := es_valid && es_ready_go
    when (io.es_allowin) {
        es_valid := io.ds_to_es_valid
    }
    when (io.ds_to_es_valid && io.es_allowin) {
        ds_to_es_bus_r := io.ds_to_es_bus
    }

    // val alu_op          = ds_to_es_bus_r(145, 140)
    val alu_op             = ds_to_es_bus_r.alu_op
    // val es_load_op      = ds_to_es_bus_r(139)
    val es_load_op         = ds_to_es_bus_r.es_load_op
    // val src1_is_pc      = ds_to_es_bus_r(138)
    val src1_is_pc         = ds_to_es_bus_r.src1_is_pc.asBool
    // val src2_is_imm     = ds_to_es_bus_r(137)
    val src2_is_imm        = ds_to_es_bus_r.src2_is_imm.asBool
    // val src2_is_4       = ds_to_es_bus_r(136)
    val src2_is_4          = ds_to_es_bus_r.src2_is_4.asBool
    // val gr_we           = ds_to_es_bus_r(135)
    val gr_we              = ds_to_es_bus_r.gr_we
    // val es_mem_we       = ds_to_es_bus_r(134)
    val es_mem_we          = ds_to_es_bus_r.es_mem_we.asBool
    // val dest            = ds_to_es_bus_r(133, 129)
    val dest              = ds_to_es_bus_r.dest
    // val imm             = ds_to_es_bus_r(128, 97)
    val imm               = ds_to_es_bus_r.imm
    // val rj_value        = ds_to_es_bus_r(96, 65)
    val rj_value          = ds_to_es_bus_r.rj_value
    // val rkd_value       = ds_to_es_bus_r(64, 33)
    val rkd_value         = ds_to_es_bus_r.rkd_value
    // val es_pc           = ds_to_es_bus_r(32, 1)
    val es_pc             = ds_to_es_bus_r.pc
    // val res_from_mem    = ds_to_es_bus_r(0)
    val res_from_mem      = ds_to_es_bus_r.res_from_mem

    // did't use in lab7
    val es_res_from_mem = es_load_op

    // 调用ALU获得运算结果
    val alu = Module(new ALU)
    alu.io.alu_op := alu_op
    alu.io.alu_src1 := Mux(src1_is_pc, es_pc, rj_value)
    alu.io.alu_src2 := Mux(src2_is_imm, imm, Mux(src2_is_4, 4.U, rkd_value))
    val alu_result = alu.io.alu_result

    //传递信息
    // io.es_to_ms_bus := Cat( res_from_mem, 
    //                         gr_we, 
    //                         dest, 
    //                         alu_result, 
    //                         es_pc)
    io.es_to_ms_bus.res_from_mem := res_from_mem
    io.es_to_ms_bus.gr_we := gr_we
    io.es_to_ms_bus.dest := dest
    io.es_to_ms_bus.alu_result := alu_result
    io.es_to_ms_bus.pc := es_pc
    io.es_to_ms_bus.mem_we := es_mem_we
    io.es_to_ms_bus.rkd_value := rkd_value

    // io.data_sram_en := true.B
    // io.data_sram_we := Mux(es_mem_we && es_valid, 4.U, 0.U)
    // // io.data_sram_addr := alu_result
    // io.data_sram_waddr := alu_result
    // io.data_sram_wdata := rkd_value
}