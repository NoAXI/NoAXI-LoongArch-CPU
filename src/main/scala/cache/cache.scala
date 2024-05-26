package cache

import chisel3._
import chisel3.util._

import bundles._
import const.cacheConst._
import const.Parameters._
import Funcs.Functions._

class CacheIO extends Bundle {
  val q = Flipped(new data_sramIO)
}

class Cache extends Module {
  val io = IO(new CacheIO)

  val banksram = SyncReadMem(LINE_WIDTH, Vec(WAY_WIDTH, Vec(LINE_SIZE, UInt(8.W))))
  val tagsram  = SyncReadMem(LINE_WIDTH, Vec(WAY_WIDTH, UInt(TAG_WIDTH.W)))
  val validreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))
  val dirtyreg = RegInit(VecInit(Seq.fill(LINE_WIDTH)(VecInit(Seq.fill(WAY_WIDTH)(false.B)))))

  val qtag    = io.q.addr(31, 12)
  val qindex  = io.q.addr(11, 4)
  val qoffset = io.q.addr(3, 0)

  val data  = RegInit(VecInit(Seq.fill(WAY_WIDTH)(VecInit(Seq.fill(LINE_SIZE)(0.U(8.W))))))
  val tag   = RegInit(VecInit(Seq.fill(WAY_WIDTH)(0.U(TAG_WIDTH.W))))
  val valid = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val dirty = RegInit(VecInit(Seq.fill(WAY_WIDTH)(false.B)))
  val hit   = WireDefault(VecInit(Seq.fill(WAY_WIDTH)(false.B)))

  // read information from cache ram
  for (i <- 0 until WAY_WIDTH) {
    data(i)  := banksram.read(qindex, io.q.en)(i)
    tag(i)   := tagsram.read(qindex, io.q.en)(i)
    valid(i) := validreg(qindex)(i)
    dirty(i) := dirtyreg(qindex)(i)
  }

  // check hit
  for (i <- 0 until WAY_WIDTH) {
    when(valid(i) && tag(i) === qtag) {
      hit(i) := true.B
      io.q.rdata := MateDefault(
        qoffset(3, 2),
        0.U,
        Seq(
          0.U -> Cat(data(i).slice(0, 4)),
          1.U -> Cat(data(i).slice(4, 8)),
          2.U -> Cat(data(i).slice(8, 12)),
          3.U -> Cat(data(i).slice(12, 16)),
        ),
      )
    }
  }

}

/*
Cache并不会立即更新内存中的内容，而是等到这个 Cache line因为某种原因需要从 Cache中移除时, Cacheオ更新内存中的数据

dirty标志位表示Cache的内容和内存的内容是否一致

当一个dirty状态的cache line miss了，可以先把该dirty的cache line用reg（写缓存）存起来，先进行replace，再择机把reg中存的写回数据写到内存中
但需要注意的是，如果读取cache但miss，首先需要去写缓存中找是否有这个数据
 */
