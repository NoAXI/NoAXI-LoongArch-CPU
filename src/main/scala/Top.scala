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
import chisel3.util.experimental.BoringUtils
import chisel3.WireDefaultImpl

class DebugIO extends Bundle {
  val wb_pc       = Output(UInt(32.W))
  val wb_rf_we    = Output(UInt(4.W))
  val wb_rf_wnum  = Output(UInt(5.W))
  val wb_rf_wdata = Output(UInt(32.W))
}

class StatisticIO extends Bundle {
  val branch_succeed_time = Output(UInt(32.W))
  val branch_total_time   = Output(UInt(32.W))
  val iCache_succeed_time = Output(UInt(32.W))
  val iCache_total_time   = Output(UInt(32.W))
  val dCache_succeed_time = Output(UInt(32.W))
  val dCache_total_time   = Output(UInt(32.W))
}

class TopIO extends Bundle {
  val ext_int = Input(UInt(8.W))
  val axi     = new AXIIO
  val debug   = new DebugIO
  val debug1  = if (Config.debug_on_chiplab) Some(new DebugIO) else None
  // val gpr            = if (Config.debug_on_chiplab) Some(Output(Vec(AREG_NUM, UInt(DATA_WIDTH.W)))) else None
  val statistic      = if (Config.statistic_on) Some(new StatisticIO) else None
  val debug_uncached = if (Config.debug_on) Some(new DebugIO) else None

  val diff = if (Config.debug_on_chiplab) Some(Output(new CommitBundle)) else None
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
  val stallCtrl = Module(new StallCtrl).io
  val bpu       = Module(new BPU).io
  val csr       = Module(new CSR).io

  // backend unit
  val rat           = Module(new Rat).io
  val rob           = Module(new Rob).io
  val preg          = Module(new PReg).io
  val forward       = Module(new Forward).io
  val stableCounter = Module(new StableCounter).io

  // memory access
  val axilayer    = Module(new AXILayer).io
  val iCache      = Module(new ICache).io
  val dcache      = Module(new DCache).io
  val tlb         = Module(new TLB).io
  val storeBuffer = Module(new StoreBuffer(STORE_BUFFER_LENGTH)).io
  val memorySel   = Module(new MemorySelect).io

  // ==================== stage connect ====================
  // prefetch -> ... -> dispatch
  prefetch.from.bits               := 0.U.asTypeOf(prefetch.from.bits)
  prefetch.from.valid              := RegNext(!reset.asBool) & !reset.asBool
  prefetch.predictResFromFront     := predecode.predictRes
  prefetch.predictResFromPredictor := fetch.predict
  prefetch.flush                   := flushCtrl.frontFlush || predecode.flushapply // TODO: 优先级

  prefetch.to  <> fetch.from
  fetch.to     <> predecode.from
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
  memory0.to        <> memorySel.fromMem0
  memory1.to        <> memory2.from
  memory2.to        <> writeback(3).from
  memory2.cacOpInfo <> iCache.cacop

  // always set write-ready high for wb stage
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    writeback(i).to.ready := true.B
  }

  // ==================== components connect ====================
  // axi connect
  axilayer.icache <> iCache.axi
  axilayer.dcache <> dcache.axi
  axilayer.to     <> io.axi

  // tlb <> prefetch, fetch, memory0, memory1, csr
  tlb.stage0(0) <> prefetch.tlb
  tlb.stage1(0) <> fetch.tlb
  tlb.csr       <> csr.tlb
  tlb.stage0(1) <> memory0.tlb
  tlb.stage1(1) <> memory1.tlb

  // icache <> fetch, prefetch
  prefetch.iCache <> iCache.preFetch
  fetch.iCache    <> iCache.fetch

  // dcache <> memory0, memory1, memory2
  memory1.dCache <> dcache.mem1
  memory2.dCache <> dcache.mem2
  memory2.mem1   <> memory1.mem2

  // storeBuffer <> memory1, memory2
  storeBuffer.memory2 <> memory2.storeBufferRead
  storeBuffer.from    <> memory2.storeBufferWrite

  // bpu <> prefetch, fetch
  bpu.preFetch <> prefetch.bpu
  bpu.fetch    <> fetch.bpu

  // rename <> rat
  rename.ratRename <> rat.rename
  rename.ratRead   <> rat.read
  rename.ratFull   <> rat.empty

  // rename <> rob
  rename.rob      <> rob.rename
  rename.robStall <> rob.renameStall

  // issue
  issue.arithSize     <> dispatch.arithSize
  issue.busyInfo      <> dispatch.busyInfo
  issue.committedBusy <> storeBuffer.committedBusy

  // readreg <> forward, preg, issue
  for (i <- 0 until BACK_ISSUE_WIDTH) {
    readreg(i).forwardReq <> forward.req(i)
    readreg(i).pregRead   <> preg.read(i)
    if (i < ARITH_ISSUE_NUM) {
      readreg(i).awake <> issue.awake(i)
    }
  }
  muldiv1.awake                    <> issue.awake(MULDIV_ISSUE_ID)
  writeback(MEMORY_ISSUE_ID).awake <> issue.awake(MEMORY_ISSUE_ID)

  // forward <> the last stage of execute
  for (i <- 0 until ARITH_ISSUE_NUM) {
    arith(i).forward <> forward.exe(i)
  }
  muldiv2.forward <> forward.exe(MULDIV_ISSUE_ID)
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
  when(clock.asBool) {
    io.debug := commit.debug(0)
  }.otherwise {
    io.debug := commit.debug(1)
  }
  // if (Config.debug_on_chiplab) {

  //   val debugVec = RegInit(VecInit.fill(2)(0.U.asTypeOf(new Bundle {
  //     val wb_pc       = UInt(32.W)
  //     val wb_rf_we    = UInt(4.W)
  //     val wb_rf_wnum  = UInt(5.W)
  //     val wb_rf_wdata = UInt(32.W)
  //   })))

  //   for (i <- 0 until 2) {
  //     when(commit.debug(i).wb_rf_we.orR) {
  //       debugVec(i).wb_pc       := commit.debug(i).wb_pc
  //       debugVec(i).wb_rf_we    := commit.debug(i).wb_rf_we
  //       debugVec(i).wb_rf_wnum  := commit.debug(i).wb_rf_wnum
  //       debugVec(i).wb_rf_wdata := commit.debug(i).wb_rf_wdata
  //     }
  //   }

  //   io.debug          := debugVec(0)
  //   io.debug.wb_rf_we := RegNext(commit.debug(0).wb_rf_we)

  //   io.debug1.get          := debugVec(1)
  //   io.debug1.get.wb_rf_we := RegNext(commit.debug(1).wb_rf_we)
  // }

  // store roll back
  storeBuffer.popValid := commit.bufferPopValid
  commit.bufferToReady := storeBuffer.to.ready
  storeBuffer.to       <> memorySel.fromBuffer
  memorySel.to         <> memory1.from

  // front flush
  prefetch.flush       := flushCtrl.frontFlush || predecode.flushapply
  fetch.flush          := flushCtrl.frontFlush || predecode.flushapply
  flushCtrl.frontFlush <> predecode.flush
  flushCtrl.frontFlush <> ib.flush
  flushCtrl.backFlush  <> decode.flush
  flushCtrl.backFlush  <> rename.flush

  // recover flush
  flushCtrl.recover <> rob.flush
  flushCtrl.recover <> rat.flush
  flushCtrl.recover <> storeBuffer.flush
  flushCtrl.recover <> commit.flush

  // stall
  issue.memoryStall      := false.B
  stallCtrl.frontStall   <> ib.stall
  stallCtrl.stallType    <> commit.stallType
  stallCtrl.stallInfo    <> commit.stallInfo
  stallCtrl.cacopSignal  <> writeback(MEMORY_ISSUE_ID).cacopDone
  stallCtrl.idleSignal   <> csr.intExc
  stallCtrl.stallRecover <> flushCtrl.stallRecover
  stallCtrl.llSignal     <> writeback(MEMORY_ISSUE_ID).llDone

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

  flushCtrl.backFlush <> memory0.flush
  flushCtrl.recover   <> memory1.flush
  flushCtrl.recover   <> memory2.flush

  for (i <- 0 until BACK_ISSUE_WIDTH) {
    flushCtrl.backFlush <> writeback(i).flush
  }

  // flush control: branch
  flushCtrl.flushInfo   <> commit.flushInfo
  flushCtrl.flushTarget <> prefetch.flushTarget
  flushCtrl.commitStall <> commit.stall
  commit.predictResult  <> prefetch.predictResFromBack

  // csr
  csr.csrRead     <> muldiv0.csrRead
  csr.csrWrite    <> muldiv0.csrWrite
  csr.excJump     <> commit.excJump
  csr.excHappen   <> commit.excHappen
  csr.intExc      <> decode.intExc
  csr.ext_int     <> io.ext_int
  csr.llbit       <> memory1.llbit
  csr.writeLLBCTL <> writeback(MEMORY_ISSUE_ID).writeLLBCTL

  muldiv0.commitCsrWriteDone <> commit.csrWritePop

  // tlb
  tlb.exe               <> muldiv0.tlbBufferInfo
  tlb.csr               <> csr.tlb
  muldiv0.commitTlbDone <> commit.tlbBufferPop

  // cnt
  for (i <- 0 until ARITH_ISSUE_NUM) {
    stableCounter.counter <> arith(i).stableCounter
  }

  if (Config.statistic_on) {
    io.statistic.get.branch_succeed_time := BoringUtils.bore(bpu.succeed_time.get)
    io.statistic.get.branch_total_time   := BoringUtils.bore(bpu.total_time.get)
    io.statistic.get.iCache_succeed_time := BoringUtils.bore(iCache.succeed_time.get)
    io.statistic.get.iCache_total_time   := BoringUtils.bore(iCache.total_time.get)
    io.statistic.get.dCache_succeed_time := BoringUtils.bore(dcache.succeed_time.get)
    io.statistic.get.dCache_total_time   := BoringUtils.bore(dcache.total_time.get)
  }
  if (Config.debug_on) {
    io.debug_uncached.get := writeback(MEMORY_ISSUE_ID).debug_uncached.get
  }

  if (Config.debug_on_chiplab) {
    preg.debug_rat <> rat.debug_rat

    //   // val commitQueue  = Module(new Queue(new CommitBundle, 128)).io
    //   // when(commit.debug(0).wb_rf_we.orR && clock.asBool) {
    //   //   commitQueue.enq.bits  := commitBundle
    //   //   commitQueue.enq.valid := true.B
    //   // }
    //   // when(commit.debug(1).wb_rf_we.orR && ~clock.asBool) {
    //   //   commitQueue.enq.bits  := commitBundle
    //   //   commitQueue.enq.valid := true.B
    //   // }

    //   // when(commitQueue.deq.valid) {
    //   //   io.c.get              := commitQueue.deq.bits
    //   //   commitQueue.deq.ready := true.B
    //   // }

    val commitBundle = WireDefault(0.U.asTypeOf(new CommitBundle))
    val commitID     = commit.debug_chiplab.get(1).DifftestInstrCommit.valid
    commitBundle.DifftestInstrCommit := RegNext(commit.debug_chiplab.get(commitID).DifftestInstrCommit)
    commitBundle.DifftestExcpEvent   := RegNext(commit.debug_chiplab.get(commitID).DifftestExcpEvent)
    commitBundle.DifftestLoadEvent   := RegNext(commit.debug_chiplab.get(commitID).DifftestLoadEvent)
    commitBundle.DifftestStoreEvent  := RegNext(commit.debug_chiplab.get(commitID).DifftestStoreEvent)
    commitBundle.DifftestTrapEvent   := RegNext(commit.debug_chiplab.get(commitID).DifftestTrapEvent)

    commitBundle.DifftestGRegState.coreid := 0.U
    commitBundle.DifftestGRegState.gpr    := preg.debug_gpr
    commitBundle.DifftestCSRRegState      := csr.csrRegs.get
    commitBundle.DifftestExcpEvent.intrNo := csr.csrRegs.get.estat(12, 2)

    io.debug      := commit.debug(0)
    io.debug1.get := commit.debug(1)

    io.diff.get := commitBundle
  }
}
