package pipeline

import chisel3._
import chisel3.util._

import const.Parameters._

class IssueTopIO extends Bundle {}
class IssueTop extends Module {
  val io = IO(new IssueTopIO)
}
