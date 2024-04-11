// // import Mill dependency
// import mill._
// import mill.define.Sources
// import mill.modules.Util
// import mill.scalalib.TestModule.ScalaTest
// import scalalib._
// // support BSP
// import mill.bsp._

// object T1 extends SbtModule { m =>
//   override def millSourcePath = os.pwd  // 当前路径
//   override def scalaVersion = "2.13.12"
//   override def scalacOptions = Seq(
//     "-language:reflectiveCalls",
//     "-deprecation",
//     "-feature",
//     "-Xcheckinit",
//   )
//   override def ivyDeps = Agg(  // 指定了项目的依赖库
//     ivy"org.chipsalliance::chisel:6.2.0",
//     // ivy"edu.berkeley.cs::chiseltest:0.6.0"  // 对应的chisel版本是3.5.2
//   )
//   override def scalacPluginIvyDeps = Agg(  // 插件依赖
//     ivy"org.chipsalliance:::chisel-plugin:6.2.0",
//   )
//   // 测试用
//   object test extends SbtModuleTests with TestModule.ScalaTest {
//     override def ivyDeps = m.ivyDeps() ++ Agg(
//       ivy"org.scalatest::scalatest::3.2.16"
//     )
//   }
// }


// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.TestModule.ScalaTest
import scalalib._
// support BSP
import mill.bsp._

object LA64 extends SbtModule { m =>
  override def millSourcePath = os.pwd
  override def scalaVersion = "2.13.8"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit",
    "-P:chiselplugin:genBundleElements"
  )
  override def ivyDeps = Agg(
    ivy"edu.berkeley.cs::chisel3:3.5.1",
  )
  override def scalacPluginIvyDeps = Agg(
    ivy"edu.berkeley.cs:::chisel3-plugin:3.5.1",
  )
  object test extends SbtModuleTests with TestModule.ScalaTest {
    override def ivyDeps = m.ivyDeps() ++ Agg(
      ivy"edu.berkeley.cs::chiseltest:0.5.1"
    )
  }
}
