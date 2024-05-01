import stages.Top

object Elaborate extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  circt.stage.ChiselStage.emitSystemVerilogFile(new Top(), args, firtoolOptions)
}