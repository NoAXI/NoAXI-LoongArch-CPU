package controller

import chisel3._
import chisel3.util._

import config._
import config.Functions._

class hazardData extends Bundle with Parameters {
  val we   = Bool()
  val addr = UInt(ADDR_WIDTH_REG.W)
  val data = UInt(DATA_WIDTH.W)
}

class dsRegInfo extends Bundle with Parameters {
  val addr         = Vec(2, UInt(ADDR_WIDTH_REG.W))
  val csr_addr     = UInt(14.W)
  val ini_data     = Vec(2, UInt(DATA_WIDTH.W))
  val csr_ini_data = UInt(DATA_WIDTH.W)
}

class dsRegData extends Bundle with Parameters {
  val data    = Vec(2, UInt(DATA_WIDTH.W))
  val csr_val = UInt(DATA_WIDTH.W)
}

class controller_IO extends Bundle {
  val es     = Input(new hazardData)
  val ms     = Input(new hazardData)
  val ws     = Input(new hazardData)
  val csr_es = Input(new hazardData)
  val csr_ms = Input(new hazardData)
  val csr_ws = Input(new hazardData)

  val ds_reg_info = Input(new dsRegInfo)
  val ds_reg_data = Output(new dsRegData)

  val flush_en    = Output(Vec(5, Bool()))
  val flush_apply = Input(Vec(5, UInt(5.W)))
}

class controller extends Module {
  val io = IO(new controller_IO)

  // flush control
  io.flush_en := MuxCase(
    io.flush_apply(4),
    Seq(
      (io.flush_apply(4).asUInt =/= 0.U) -> io.flush_apply(4),
      (io.flush_apply(3).asUInt =/= 0.U) -> io.flush_apply(3),
      (io.flush_apply(2).asUInt =/= 0.U) -> io.flush_apply(2),
      (io.flush_apply(1).asUInt =/= 0.U) -> io.flush_apply(1),
      (io.flush_apply(0).asUInt =/= 0.U) -> io.flush_apply(0),
    ),
  ).asBools

  // csr register 前递
  val addr = io.ds_reg_info.csr_addr
  io.ds_reg_data.csr_val := MuxCase(
    io.ds_reg_info.csr_ini_data,
    List(
      (addr === io.csr_es.addr && io.csr_es.we) -> io.csr_es.data,
      (addr === io.csr_ms.addr && io.csr_ms.we) -> io.csr_ms.data,
      (addr === io.csr_ws.addr && io.csr_ws.we) -> io.csr_ws.data,
    ),
  )

  // general register 前递
  for (i <- 0 until 2) {
    val addr = io.ds_reg_info.addr(i)
    io.ds_reg_data.data(i) := MuxCase(
      io.ds_reg_info.ini_data(i),
      List(
        (addr === io.es.addr && io.es.we && addr =/= 0.U) -> io.es.data,
        (addr === io.ms.addr && io.ms.we && addr =/= 0.U) -> io.ms.data,
        (addr === io.ws.addr && io.ws.we && addr =/= 0.U) -> io.ws.data,
      ),
    )
  }
}
