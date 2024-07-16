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
}
class Top extends Module {
  val io = IO(new TopIO)

  // ==================== pipline define ====================
  // frontend
  val prefetch  = Module(new PrefetchTop).io
  val fetch     = Module(new FetchTop).io
  val predecode = Module(new PreDecodeTop).io
  val ib        = Module(new InstBuffer).io
  val decode    = Module(new DecodeTop).io
  val rename    = Module(new RenameTop).io

  // backend before execute
  val dispatch = Module(new DispatchTop).io
  val issue    = Module(new IssueTop).io
  val readreg = (
    Seq.fill(ARITH_ISSUE_NUM)(Module(new ReadRegTop("arith")).io) ++
      Seq(Module(new ReadRegTop("muldiv")).io) ++
      Seq(Module(new ReadRegTop("memory")).io)
  )

  // backend execute
  val arith = Seq(
    Module(new ArithmeticTop(hasBru = true)).io,
    Module(new ArithmeticTop(hasBru = true)).io,
  )

  val muldiv0 = Module(new Muldiv0Top).io
  val muldiv1 = Module(new Muldiv1Top).io
  val muldiv2 = Module(new Muldiv2Top).io

  val memory0 = Module(new Memory0Top).io
  val memory1 = Module(new Memory1Top).io
  val memory2 = Module(new Memory2Top).io

  // backend after execute
  val writeback = (
    Seq.fill(BACK_ISSUE_WIDTH - 1)(Module(new WritebackTop).io) ++
      Seq(Module(new WritebackTop("memory")).io)
  )
  val commit = Module(new CommitTop).io

  // ==================== unit define ====================
  val flushCtrl = Module(new FlushCtrl).io
  val bpu       = Module(new BPU).io
  val csr       = Module(new CSR).io

  // backend unit
  val rat     = Module(new Rat).io
  val rob     = Module(new Rob).io
  val preg    = Module(new PReg).io
  val forward = Module(new Forward).io

  // memory access
  val axilayer    = Module(new AXILayer).io
  val iCache      = Module(new ICache).io
  val dcache      = Module(new DCache).io
  val itlb        = Module(new TLB("fetch")).io
  val dtlb        = Module(new TLB("memory")).io
  val storeBuffer = Module(new StoreBuffer(STORE_BUFFER_LENGTH)).io
  val memorySel   = Module(new MemorySelect).io

  // ==================== stage connect ====================
  // prefetch -> ... -> dispatch
  prefetch.from.bits           := 0.U.asTypeOf(prefetch.from.bits)
  prefetch.from.valid          := RegNext(!reset.asBool) & !reset.asBool
  prefetch.predictResFromFront := predecode.predictRes
  prefetch.flush               := flushCtrl.frontFlush || predecode.flushapply // TODO: 优先级

  prefetch.to  <> fetch.from
  fetch.to     <> predecode.from
  fetch.flush  := flushCtrl.backFlush || predecode.flushapply
  predecode.to <> ib.from
  ib.to        <> decode.from
  decode.to    <> rename.from
  rename.to    <> dispatch.from

  // dispatch -> issue -> readreg
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

  // arith
  arith(0).to <> writeback(0).from
  arith(1).to <> writeback(1).from

  // muldiv
  muldiv0.to  <> muldiv1.from
  muldiv1.to  <> muldiv2.from
  muldiv2.to  <> writeback(2).from
  muldiv2.mul <> muldiv0.mul // multiplier connect

  // memory
  memory0.to <> memorySel.fromMem0
  memory1.to <> memory2.from
  memory2.to <> writeback(3).from

  // always set write-ready high for wb stage
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    writeback(i).to.ready := true.B
  }

  // ==================== components connect ====================
  // axi connect
  axilayer.icache <> iCache.axi
  axilayer.dcache <> dcache.axi
  axilayer.to     <> io.axi

  // itlb <> prefetch, fetch, memory0, memory1, csr
  itlb.stage0 <> prefetch.tlb
  itlb.stage1 <> fetch.tlb
  itlb.csr    <> csr.tlb(0)

  // dtlb <> memory0, memory1, memory2, csr
  dtlb.stage0 <> memory0.tlb
  dtlb.stage1 <> memory1.tlb
  dtlb.csr    <> csr.tlb(1)

  // icache <> fetch, prefetch
  prefetch.iCache <> iCache.preFetch
  fetch.iCache    <> iCache.fetch

  // dcache <> memory0, memory1, memory2
  memory1.dCache <> dcache.mem1
  memory2.dCache <> dcache.mem2
  memory2.mem1   <> memory1.mem2

  // storeBuffer <> memory1, memory2
  storeBuffer.memory2  <> memory2.storeBufferRead
  storeBuffer.from     <> memory2.storeBufferWrite
  storeBuffer.to.ready := true.B // temp
  // storeBuffer.to

  // bpu <> prefetch, fetch
  bpu.preFetch <> prefetch.bpu

  // csr
  csr.csr_reg_read.re    := false.B
  csr.csr_reg_read.raddr := 0.U
  csr.exc_happen         := 0.U.asTypeOf(new excHappen) // TODO: fix
  csr.csr_write          := 0.U.asTypeOf(new CSRWrite)  // TODO: fix

  // rename <> rat
  rename.ratRename <> rat.rename
  rename.ratRead   <> rat.read
  rename.ratFull   <> rat.empty

  // rename <> rob
  rename.rob      <> rob.rename
  rename.robStall <> rob.renameStall

  // issue
  issue.arithSize <> dispatch.arithSize
  issue.busyInfo  <> dispatch.busyInfo

  // readreg <> forward, preg, issue
  // TODO: 修改唤醒接口
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    readreg(i).forwardReq <> forward.req(i)
    readreg(i).pregRead   <> preg.read(i)
    if (i < ARITH_ISSUE_NUM) {
      readreg(i).awake <> issue.awake(i)
    }
  }
  muldiv1.awake <> issue.awake(MULDIV_ISSUE_ID)
  // memory1.awake <> issue.awake(MEMORY_ISSUE_ID)
  memory2.awake <> issue.awake(MEMORY_ISSUE_ID)

  // forward <> the last stage of execute
  for (i <- 0 until ARITH_ISSUE_NUM) {
    arith(i).forward <> forward.exe(i)
  }
  muldiv2.forward <> forward.exe(MULDIV_ISSUE_ID) // TODO: 需要改为muldiv的最后一级
  memory2.forward <> forward.exe(MEMORY_ISSUE_ID)
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    writeback(i).forward <> forward.wb(i)
  }

  // writeback <> preg, rob
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    writeback(i).preg <> preg.write(i)
    writeback(i).rob  <> rob.write(i)
  }

  // commit <> rat, rob, debug
  commit.rat <> rat.commit
  commit.rob <> rob.commit
  // commit.buffer.to   <> storeBuffer.from
  // commit.buffer.from <> storeBuffer.to
  when(clock.asBool) {
    io.debug := commit.debug(0)
  }.otherwise {
    io.debug := commit.debug(1)
  }

  // store roll back
  storeBuffer.to   <> commit.buffer.from
  commit.buffer.to <> memorySel.fromBuffer
  memorySel.to     <> memory1.from

  // front flush
  prefetch.flush       := flushCtrl.frontFlush || predecode.flushapply
  fetch.flush          := flushCtrl.frontFlush || predecode.flushapply
  flushCtrl.frontFlush <> predecode.flush
  flushCtrl.frontFlush <> ib.flush
  flushCtrl.frontFlush <> decode.flush
  flushCtrl.frontFlush <> rename.flush

  // recover flush
  flushCtrl.recover <> rob.flush
  flushCtrl.recover <> rat.flush
  flushCtrl.recover <> storeBuffer.flush

  // stall
  ib.stall          := false.B
  issue.memoryStall := false.B

  // back flush
  flushCtrl.backFlush <> dispatch.flush
  flushCtrl.backFlush <> issue.flush

  for (i <- 0 until BACK_ISSUE_WIDTH) {
    flushCtrl.backFlush <> readreg(i).flush
  }

  for (i <- 0 until ARITH_ISSUE_NUM) {
    flushCtrl.backFlush <> arith(i).flush
  }

  flushCtrl.backFlush <> muldiv0.flush
  flushCtrl.backFlush <> muldiv1.flush
  flushCtrl.backFlush <> muldiv2.flush

  flushCtrl.backFlush <> memory0.flush // flush when memStall = 1
  flushCtrl.backFlush <> memory1.flush
  flushCtrl.backFlush <> memory2.flush

  for (i <- 0 until BACK_ISSUE_WIDTH) {
    flushCtrl.backFlush <> writeback(i).flush
  }

  flushCtrl.backFlush <> commit.flush

  // flush control: branch
  flushCtrl.flushInfo   <> commit.flushInfo
  flushCtrl.flushTarget <> prefetch.flushTarget
  flushCtrl.commitStall <> commit.stall
  flushCtrl.fetchStall  <> fetch.busy
  commit.predictResult  <> prefetch.predictResFromBack
}
