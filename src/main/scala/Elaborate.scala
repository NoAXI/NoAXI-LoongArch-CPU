import scala.old.pipelines.Top
import stages.Top
import configs._

object Elaborate extends App {
  val firtoolOptions = Array(
    "--lowering-options=" + List(
      "disallowLocalVariables",
      "disallowPackedArrays",
      "locationInfoStyle=wrapInAtSquareBracket",
    ).reduce(_ + "," + _),
  )
  if (!CpuConfig.hasBlackBox) {
    println("\u001b[31m !!! Generating without blackbox, only for soc-simulator !!!\u001b[0m")
  } else {
    println("\u001b[32m !!! Generating with blackbox, are you running in vivado? !!!\u001b[0m")
  }
  circt.stage.ChiselStage.emitSystemVerilogFile(new Top(), args, firtoolOptions)
}
