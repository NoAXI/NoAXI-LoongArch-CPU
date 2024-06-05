// package axi

// import chisel3._
// import chisel3.util._

// import cache._
// import bundles._
// import Funcs.Functions._
// import const.cacheConst._
// import const.Parameters._
// import configs.CpuConfig

// class dCacheIO extends Bundle {
//   val axi = new dCache_AXI
//   val exe = Flipped(new exe_dCache_IO)
//   val mem = Flipped(new _mem_dCache_IO)
// }

// class dCache extends Module {
//   val io = IO(new dCacheIO)

//   val datasram   = VecInit.fill(2)(Module(new xilinx_single_port_ram_read_first((LINE_SIZE * 8), LINE_WIDTH)).io)
//   val tagsram    = VecInit.fill(2)(Module(new xilinx_single_port_ram_read_first(TAG_WIDTH, LINE_WIDTH)).io)
//   val validreg   = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(2)(false.B)))))
//   val dirtyreg   = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(2)(false.B)))))
//   val next_valid = RegInit(VecInit(Seq.fill(2)(false.B)))
//   val next_dirty = RegInit(VecInit(Seq.fill(2)(false.B)))
//   val lru        = RegInit(VecInit(Seq.fill(LINE_WIDTH)(false.B)))

//   val ar            = RegInit(0.U.asTypeOf(new AR))
//   val arvalid       = RegInit(false.B)
//   val rready        = RegInit(false.B)
//   val aw            = RegInit(0.U.asTypeOf(new AW))
//   val awvalid       = RegInit(false.B)
//   val w             = RegInit(0.U.asTypeOf(new W))
//   val wvalid        = RegInit(false.B)
//   val bready        = true.B
//   val ans_valid_imm = WireDefault(false.B)
//   val ans_valid     = RegInit(false.B)
//   val ans_bits      = RegInit(0.U(DATA_WIDTH.W))
//   // val after_replace  = RegInit(false.B)
//   val next_addr      = RegInit(0.U(ADDR_WIDTH.W)) // the most begin: set an illegal address
//   val next_tag       = RegInit(0.U(TAG_WIDTH.W))
//   val next_index     = RegInit(0.U(8.W))
//   val next_offset    = RegInit(0.U(2.W))
//   val saved_addr     = RegInit(0.U(ADDR_WIDTH.W))
//   val saved_data     = RegInit(VecInit.fill(2)(0.U((LINE_SIZE * 8).W)))
//   val saved_tag      = RegInit(0.U(TAG_WIDTH.W))
//   val saved_index    = RegInit(0.U(8.W))
//   val saved_offset   = RegInit(0.U(2.W))
//   val saved_dirty    = RegInit(VecInit(Seq.fill(2)(false.B)))
//   val saved_hitdata  = RegInit(0.U(DATA_WIDTH.W))
//   val re             = RegInit(false.B)
//   val we             = RegInit(false.B)
//   val wdata          = RegInit(0.U((LINE_SIZE * 8).W))
//   val wmask          = RegInit(1.U(4.W))
//   val is_uncached    = RegInit(false.B)
//   val write_back     = WireDefault(false.B)
//   val is_replace_put = WireDefault(false.B)

//   // if pipeline only send a tick request
//   when(io.exe.request.valid) {
//     next_addr := io.exe.request.bits.addr
//   }
//   val cached = next_addr(31, 16) =/= 0xbfaf.U
//   is_uncached := !cached

//   // [ )'s address
//   // the exe valid should be sent in idle and hitted and not writeback
//   val now_addr  = Mux(io.exe.request.valid, io.exe.request.bits.addr, next_addr)
//   val now_index = now_addr(11, 4)

//   // prepare for the next
//   for (i <- 0 until 2) {
//     datasram(i).clka  := clock
//     datasram(i).addra := now_index
//     datasram(i).wea   := WireDefault(false.B)
//     datasram(i).dina  := 0.U
//     tagsram(i).clka   := clock
//     tagsram(i).addra  := now_index
//     tagsram(i).wea    := WireDefault(false.B)
//     tagsram(i).dina   := 0.U
//     next_valid(i)     := validreg(now_index)(i)
//     next_dirty(i)     := dirtyreg(now_index)(i)
//   }
//   next_index  := now_index
//   next_tag    := now_addr(31, 12)
//   next_offset := now_addr(3, 2)
//   val wmove     = Mux1H((3 to 0 by -1).map(i => wmask(i) -> (i * 32).U))
//   val waychosen = lru(next_index).asUInt

//   val next_hit     = VecInit.tabulate(2)(i => tagsram(i).douta === next_tag && next_valid(i))
//   val next_hitted  = next_hit.reduce(_ || _)
//   val next_hit_way = Mux(next_hit(0), 0.U, 1.U)
//   val next_hitdata = Mux1H(next_hit, VecInit.tabulate(2)(i => datasram(i).douta))
//   val hitdata = MateDefault(
//     next_offset,
//     0.U,
//     Seq(
//       0.U -> next_hitdata(31, 0),
//       1.U -> next_hitdata(63, 32),
//       2.U -> next_hitdata(95, 64),
//       3.U -> next_hitdata(127, 96),
//     ),
//   )
//   // val ans_hitted_valid = next_hitted && ShiftRegister(next_hitted, 1)

//   // statistics
//   val success = RegInit(0.U(32.W))
//   val all     = RegInit(0.U(32.W))

//   val idle :: replace_get :: replace_put :: dirty_write :: uncached_r :: uncached_w :: wait_wb :: wait_wb1 :: Nil =
//     Enum(8)

//   io.mem.answer_imm := false.B

//   val state = RegInit(idle)
//   switch(state) {
//     is(idle) { // checkhit and receive next_request
//       ans_valid := false.B
//       when(io.mem.request.fire) {
//         when(cached) {
//           // the former is hitted?
//           when(next_hitted) {
//             lru(next_index) := !lru(next_index)
//             // after_replace     := false.B
//             ans_valid         := true.B
//             io.mem.answer_imm := true.B
//             // ans_bits          := hitdata
//             write_back := io.mem.request.bits.we
//           }.otherwise {
//             state        := replace_get
//             saved_data   := VecInit.tabulate(2)(i => datasram(i).douta)
//             saved_addr   := next_addr
//             saved_tag    := next_tag
//             saved_index  := next_index
//             saved_offset := next_offset
//             arvalid      := true.B
//             ar.addr      := next_addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
//             ar.size      := 2.U
//             ar.len       := (BANK_WIDTH / 4 - 1).U
//             rready       := false.B
//             re           := io.mem.request.bits.re
//             we           := io.mem.request.bits.we
//           }
//         }.otherwise {
//           is_uncached := true.B
//           when(io.mem.request.bits.re) {
//             arvalid := true.B
//             rready  := false.B
//             ar.addr := io.mem.request.bits.addr
//             state   := uncached_r
//           }.elsewhen(io.mem.request.bits.we) {
//             awvalid := true.B
//             aw.addr := io.mem.request.bits.addr

//             wvalid := true.B
//             w.strb := io.mem.request.bits.strb
//             w.data := io.mem.request.bits.data
//             w.last := true.B

//             state := uncached_w
//           }
//         }
//       }
//     }
//     is(replace_get) {
//       when(io.axi.ar.valid) {
//         when(io.axi.ar.ready) {
//           arvalid := false.B
//           rready  := true.B
//         }
//       }.elsewhen(io.axi.r.fire) {
//         wdata := wdata | (io.axi.r.bits.data << wmove)
//         wmask := wmask << 1.U
//         when(io.axi.r.bits.last) {
//           rready := false.B
//           wmask  := 1.U
//           state  := replace_put
//         }
//       }
//     }
//     is(replace_put) {
//       when(!dirtyreg(saved_index)(waychosen)) {
//         is_replace_put    := true.B
//         write_back        := true.B
//         io.mem.answer_imm := true.B
//         state             := idle
//         wdata             := 0.U
//         wmask             := 1.U
//       }.otherwise {
//         awvalid := true.B
//         aw.addr := saved_addr(ADDR_WIDTH - 1, 4) ## 0.U(4.W)
//         aw.size := 2.U
//         aw.len  := (BANK_WIDTH / 4 - 1).U

//         wvalid := true.B
//         w.data := saved_data(waychosen)(31, 0)
//         w.strb := "b1111".U
//         w.last := false.B

//         state := dirty_write
//       }
//     }
//     is(dirty_write) {
//       when(io.axi.aw.fire) {
//         awvalid := false.B
//       }
//       when(io.axi.w.fire) {
//         when(w.last) {
//           wvalid := false.B
//         }.otherwise {
//           wmask := wmask << 1.U
//           w.data := Mux1H(
//             Seq(
//               wmask(0) -> saved_data(waychosen)(63, 32),
//               wmask(1) -> saved_data(waychosen)(95, 64),
//               wmask(2) -> saved_data(waychosen)(127, 96),
//             ),
//           )
//           w.strb := "b1111".U
//           w.last := wmask(2).asBool
//         }
//       }
//       when(io.axi.b.fire) {
//         state                            := replace_put
//         dirtyreg(saved_index)(waychosen) := false.B
//       }
//     }
//     is(uncached_r) {
//       when(io.axi.ar.valid) {
//         when(io.axi.ar.ready) {
//           arvalid := false.B
//           rready  := true.B
//         }
//       }.elsewhen(io.axi.r.fire) {
//         ans_valid := true.B
//         ans_bits  := io.axi.r.bits.data
//         state     := idle
//       }
//     }
//     is(uncached_w) {
//       when(io.axi.w.fire) {
//         wvalid := false.B
//       }
//       when(io.axi.aw.fire) {
//         awvalid := false.B
//       }
//       when(io.axi.b.fire) {
//         ans_valid := true.B
//         ans_bits  := 0.U // stand for it is no mean
//         state     := idle
//       }
//     }
//     is(wait_wb) { state := wait_wb1 }
//     is(wait_wb1) { state := idle }
//   }

//   val offset_move = MateDefault(
//     Mux(is_replace_put, saved_offset, next_offset),
//     0.U,
//     Seq(
//       1.U -> 32.U,
//       2.U -> 64.U,
//       3.U -> 96.U,
//     ),
//   )
//   val b_enable = Cat((3 to 0 by -1).map(i => Fill(8, io.mem.request.bits.strb(i))))

//   when(write_back) {
//     val chosen_way   = Mux(is_replace_put, waychosen, next_hit_way)
//     val chosen_index = Mux(is_replace_put, saved_index, next_index)
//     datasram(chosen_way).wea   := true.B
//     datasram(chosen_way).addra := chosen_index
//     datasram(chosen_way).dina := Mux(
//       is_replace_put,
//       writeMask(b_enable << offset_move, wdata, io.mem.request.bits.data << offset_move),
//       writeMask(b_enable << offset_move, next_hitdata, io.mem.request.bits.data << offset_move),
//     )
//     tagsram(chosen_way).wea   := is_replace_put
//     tagsram(chosen_way).addra := chosen_index
//     tagsram(chosen_way).dina  := Mux(is_replace_put, saved_tag, next_tag)
//     when(!is_replace_put) {
//       dirtyreg(next_index)(chosen_way) := true.B
//     }
//     validreg(chosen_index)(chosen_way) := true.B
//   }

//   ar.id := 1.U
//   // ar.len   := 0.U
//   ar.size  := 2.U
//   ar.burst := 1.U
//   ar.lock  := 0.U
//   ar.cache := 0.U
//   ar.prot  := 0.U

//   aw.id := 1.U
//   // aw.len   := 0.U
//   aw.size  := 2.U
//   aw.burst := 1.U
//   aw.lock  := 0.U
//   aw.cache := 0.U
//   aw.prot  := 0.U

//   w.id := 1.U
//   // w.last := true.B

//   io.axi.ar.bits       := ar
//   io.axi.ar.valid      := arvalid
//   io.axi.aw.bits       := aw
//   io.axi.aw.valid      := awvalid
//   io.axi.w.bits        := w
//   io.axi.w.valid       := wvalid
//   io.axi.r.ready       := rready
//   io.axi.b.ready       := bready
//   io.mem.answer.valid  := ans_valid
//   io.mem.answer.bits   := Mux(is_uncached, ans_bits, hitdata)
//   io.mem.request.ready := true.B
//   io.exe.request.ready := state === idle && !write_back // can be improve

//   if (CpuConfig.debug_on) {
//     dontTouch(hitdata)
//     dontTouch(is_uncached)
//     dontTouch(b_enable)
//     dontTouch(next_offset)
//     dontTouch(now_addr)
//     dontTouch(now_index)
//     dontTouch(next_valid)
//   }
// }
// /*
// now the answer valid signal not used
// note that when writeback can not send addr
//  */
