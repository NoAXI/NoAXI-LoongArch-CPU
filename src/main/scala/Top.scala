package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import const.Parameters._

import csr._
import controller._

import memory.tlb._
import memory.cache._

class DebugIO extends Bundle {
  val wb_pc       = Output(UInt(32.W))
  val wb_rf_we    = Output(UInt(4.W))
  val wb_rf_wnum  = Output(UInt(5.W))
  val wb_rf_wdata = Output(UInt(32.W))
}

class TopIO extends Bundle {
  val axi   = new AXIIO
  val debug = new DebugIO
  // val debug_wb_pc       = Output(UInt(32.W))
  // val debug_wb_rf_we    = Output(UInt(4.W))
  // val debug_wb_rf_wnum  = Output(UInt(5.W))
  // val debug_wb_rf_wdata = Output(UInt(32.W))
}

class Top extends Module {
  val io = IO(new TopIO)

  // ==================== pipline define ====================
  // frontend
  // val prefetch = Module(new PrefetchTop).io
  // val fetch    = Module(new FetchTop).io
  // val decode   = Module(new DecodeTop).io
  val rename = Module(new RenameTop).io

  // backend before execute
  val dispatch = Module(new DispatchTop).io
  val issue    = Module(new IssueTop).io
  val readreg = Seq.fill(ARITH_ISSUE_NUM)(Module(new ReadRegTop(isArithmetic = true)).io) ++
    Seq.fill(BACK_ISSUE_WIDTH - ARITH_ISSUE_NUM)(Module(new ReadRegTop(isArithmetic = false)).io)

  // backend execute
  val arith   = Seq.fill(ARITH_ISSUE_NUM)(Module(new ArithmeticTop).io)
  val muldiv0 = Module(new MuldivTop).io
  val memory0 = Module(new MuldivTop).io

  // backend after execute
  val writeback = Seq.fill(BACK_ISSUE_WIDTH)(Module(new WritebackTop).io)
  val commit    = Module(new CommitTop).io

  // ==================== unit define ====================
  val rat     = Module(new Rat).io
  val rob     = Module(new Rob).io
  val preg    = Module(new PReg).io
  val forward = Module(new Forward).io // exe = 0, wb = 1

  // memory access
  val axilayer = Module(new AXILayer).io
  // val icache   = Module(new ICache).io
  // val dcache   = Module(new dCache_with_cached_writebuffer).io

  // ==================== wire links ====================
  // stage connect
  // TODO: 添加前端流水线连线
  // rename -> dispatch -> issue -> readreg
  rename.to <> dispatch.from
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    dispatch.to(i) <> issue.from(i)
  }
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    issue.to(i) <> readreg(i).from
  }

  // readreg -> execute
  readreg(0).to <> arith(0).from
  readreg(1).to <> arith(1).from
  readreg(2).to <> muldiv0.from
  readreg(3).to <> memory0.from

  // execute -> writeback
  // TODO: 这里的muldiv和memory需要修改为对应流水线的最后一级
  arith(0).to <> writeback(0).from
  arith(1).to <> writeback(1).from
  muldiv0.to  <> writeback(2).from
  memory0.to  <> writeback(3).from

  // rename <> rat
  rename.ratRename <> rat.rename
  rename.ratRead   <> rat.read
  rename.ratFull   <> rat.full

  // rename <> rob
  rename.rob     <> rob.rename
  rename.robFull <> rob.full

  // readreg <> forward, preg, issue
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    readreg(i).forwardReq <> forward.req(i)
    readreg(i).pregRead   <> preg.read(i)
    if (i < ARITH_ISSUE_NUM) {
      readreg(i).awake <> issue.awake(i)
    }
  }

  // forward <> the last stage of execute
  // TODO: 需要添加muldiv和memory的前递信号
  arith(0).forward <> forward.info(0)(0)
  arith(1).forward <> forward.info(0)(1)

  // writeback <> preg, rob, forward
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    writeback(i).preg    <> preg.write(i)
    writeback(i).rob     <> rob.write(i)
    writeback(i).forward <> forward.info(1)(i)
  }

  // commit <> rat, rob, debug
  commit.rat <> rat.commit
  commit.rob <> rob.commit
  when(clock.asBool) {
    io.debug := commit.debug(0)
  }.otherwise {
    io.debug := commit.debug(1)
  }

  // axi
  io.axi <> axilayer.to
  // axilayer.icache <> icache.axi
  // axilayer.dcache <> dcache.axi
}
