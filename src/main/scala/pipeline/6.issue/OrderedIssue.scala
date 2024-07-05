package pipeline

import chisel3._
import chisel3.util._

import const._
import bundles._
import func.Functions._
import const.Parameters._

class OrderedIssueIO extends Bundle {
}

class OrderedIssue extends Module {
  val io = IO(new OrderedIssueIO)

}