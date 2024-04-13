package stages

import chisel3._
import chisel3.util._

import config._

class IF_ID_IO extends Bundle with Parameters {
    val ds_allowin = Input(Bool())  // decoder stage允许输入
    val br_bus = Input(UInt(BR_BUS_WIDTH.W))  // branch总线输入

    val fs_to_ds_valid = Output(Bool())  // fetch stage to decoder stage有效
    val fs_to_ds_bus = Output(UInt(FS_TO_DS_BUS_WIDTH.W))  // fetch stage to decoder stage总线

    val inst_sram_en = Output(Bool())  // 指令SRAM使能
    val inst_sram_we = Output(UInt(INST_WIDTH_B.W))  // 指令SRAM按字节写使能
    val inst_sram_addr = Output(UInt(ADDR_WIDTH.W))  // 指令SRAM地址
    val inst_sram_wdata = Output(UInt(INST_WIDTH.W))  // 指令SRAM写数据
    val inst_sram_rdata = Input(UInt(INST_WIDTH.W))  // 指令SRAM读数据
}

class IF_ID extends Module with Parameters {
    val io = IO(new IF_ID_IO)

    val fs_inst = io.inst_sram_rdata
    val fs_pc = RegInit(0x1c000000.U(32.W))
    val fs_valid = RegInit(false.B)

    //assign {br_taken, br_target} = br_bus;
    //why
    val br_taken = io.br_bus(0)
    val br_target = io.br_bus(32, 1)

    //assign fs_to_ds_bus = {fs_inst ,fs_pc};
    io.fs_to_ds_bus := Cat(fs_inst, fs_pc)

    //assign to_fs_valid  = ~reset;
    val to_fs_valid = true.B

    // assign seq_pc       = fs_pc + 3'h4;
    // assign nextpc       = br_taken ? br_target : seq_pc; 
    val seq_pc = fs_pc + 4.U
    val nextpc = Mux(br_taken, br_target, seq_pc)

    // IF stage
    /*
    assign fs_ready_go    = 1'b1;   // 准备发送
    assign fs_allowin     = !fs_valid || fs_ready_go && ds_allowin;     // 可接收数据（不阻塞
    assign fs_to_ds_valid =  fs_valid && fs_ready_go;   
    always @(posedge clk) begin
        if (reset) begin
            fs_valid <= 1'b0;
        end
        else if (fs_allowin) begin
            fs_valid <= to_fs_valid;    // 数据有效
        end
    end
    */
    val fs_ready_go = true.B
    val fs_allowin = !fs_valid || fs_ready_go && io.ds_allowin
    io.fs_to_ds_valid := fs_valid && fs_ready_go
    when (fs_allowin) {
        fs_valid := to_fs_valid
    }

    /*
    always @(posedge clk) begin
        if (reset) begin
            fs_pc <= 32'h1bfffffc;     //trick: to make nextpc be 0x1c000000 during reset 
        end
        else if (to_fs_valid && fs_allowin) begin
            fs_pc <= nextpc;
        end
    end
    */
    when (to_fs_valid && fs_allowin) {
        fs_pc := nextpc
    }

    // assign inst_sram_en    = to_fs_valid && fs_allowin;
    io.inst_sram_en := to_fs_valid && fs_allowin

    // assign inst_sram_we   = 4'h0;
    io.inst_sram_we := 0.U

    // assign inst_sram_addr  = nextpc;
    io.inst_sram_addr := nextpc

    // assign inst_sram_wdata = 32'b0;
    io.inst_sram_wdata := 0.U
}