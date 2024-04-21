package stages

import chisel3._
import chisel3.util._

class ds_to_es_bus extends Bundle {
    val alu_op          = UInt(6.W)
    // val es_load_op      = ds_to_es_bus_r(139)
    val es_load_op      = UInt(1.W)
    // val src1_is_pc      = ds_to_es_bus_r(138)
    val src1_is_pc      = UInt(1.W)
    // val src2_is_imm     = ds_to_es_bus_r(137)
    val src2_is_imm     = UInt(1.W)
    // val src2_is_4       = ds_to_es_bus_r(136)
    val src2_is_4       = UInt(1.W)
    // val gr_we           = ds_to_es_bus_r(135)
    val gr_we           = UInt(1.W)
    // val es_mem_we       = ds_to_es_bus_r(134)
    val es_mem_we       = UInt(1.W)
    // val dest            = ds_to_es_bus_r(133, 129)
    val dest            = UInt(5.W)
    // val imm             = ds_to_es_bus_r(128, 97)
    val imm             = UInt(32.W)
    // val rj_value        = ds_to_es_bus_r(96, 65)
    val rj_value        = UInt(32.W)
    // val rkd_value       = ds_to_es_bus_r(64, 33)
    val rkd_value       = UInt(32.W)
    // val es_pc           = ds_to_es_bus_r(32, 1)
    val pc           = UInt(32.W)
    // val res_from_mem    = ds_to_es_bus_r(0)
    val res_from_mem    = UInt(1.W)
}

class es_to_ms_bus extends Bundle {
    val res_from_mem = UInt(1.W)
    val gr_we        = UInt(1.W)
    val dest         = UInt(5.W)
    val alu_result   = UInt(32.W)
    val pc        = UInt(32.W)
    val mem_we     = UInt(1.W)
    val rkd_value  = UInt(32.W)
}

class ms_to_ws_bus extends Bundle {
    val gr_we        = UInt(1.W)
    val dest         = UInt(5.W)
    val final_result = UInt(32.W)
    val pc           = UInt(32.W)
    val ms_res_from_mem = UInt(1.W)
}