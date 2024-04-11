module ALU(
  input         clock,
  input         reset,
  input  [31:0] io_instruction,
  input  [63:0] io_rj,
  input  [63:0] io_rk,
  input  [63:0] io_rd_in,
  input  [63:0] io_imm,
  output [63:0] io_rd
);
  assign io_rd = 64'h0; // @[ALU.scala 29:11]
endmodule
