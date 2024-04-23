package stages

import chisel3._
import chisel3.util._

class fs_to_ds_bus extends Bundle {
    val pc = UInt(32.W)
    val inst = UInt(32.W)
}

class ds_to_es_bus extends Bundle {
    val alu_op          = UInt(6.W)
    val es_load_op      = UInt(1.W)
    val src1_is_pc      = UInt(1.W)
    val src2_is_imm     = UInt(1.W)
    val src2_is_4       = UInt(1.W)
    val gr_we           = UInt(1.W)
    val es_mem_we       = UInt(1.W)
    val dest            = UInt(5.W)
    val imm             = UInt(32.W)
    val rj_value        = UInt(32.W)
    val rkd_value       = UInt(32.W)
    val pc           = UInt(32.W)
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

class br_bus extends Bundle {
    val br_taken = Bool()
    val br_target = UInt(32.W)
}

class info extends {
    val pc              = UInt(32.W)
    val inst            = UInt(32.W)
    val alu_op          = UInt(6.W)
    val load_op         = UInt(1.W)
    val src1_is_pc      = UInt(1.W)
    val src2_is_imm     = UInt(1.W)
    val src2_is_4       = UInt(1.W)
    val gr_we           = UInt(1.W)
    val mem_we          = UInt(1.W)
    val dest            = UInt(5.W)
    val imm             = UInt(32.W)
    val rj_value        = UInt(32.W)
    val rkd_value       = UInt(32.W)
    val res_from_mem    = UInt(1.W)
    val alu_result      = UInt(32.W)
}