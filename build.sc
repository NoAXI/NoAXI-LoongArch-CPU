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
// import Mill dependency
import mill._
import mill.define.Sources
import mill.modules.Util
import mill.scalalib.scalafmt.ScalafmtModule
import mill.scalalib.TestModule.ScalaTest
import mill.scalalib._
// support BSP
import mill.bsp._

object playground extends SbtModule with ScalafmtModule { m =>
  val useChisel3 = false
  override def millSourcePath = os.pwd / "src"
  override def scalaVersion = "2.13.12"
  override def scalacOptions = Seq(
    "-language:reflectiveCalls",
    "-deprecation",
    "-feature",
    "-Xcheckinit"
  )
  override def sources = T.sources {
    super.sources() ++ Seq(PathRef(millSourcePath / "main"))
  }
  override def ivyDeps = Agg(
    if (useChisel3) ivy"edu.berkeley.cs::chisel3:3.6.0" else
    ivy"org.chipsalliance::chisel:7.0.0-M1"
  )
  override def scalacPluginIvyDeps = Agg(
    if (useChisel3) ivy"edu.berkeley.cs:::chisel3-plugin:3.6.0" else
    ivy"org.chipsalliance:::chisel-plugin:7.0.0-M1"
  )
  object test extends SbtModuleTests with TestModule.ScalaTest with ScalafmtModule {
    override def sources = T.sources {
      super.sources() ++ Seq(PathRef(millSourcePath / "test"))
    }
    override def ivyDeps = super.ivyDeps() ++ Agg(
      if (useChisel3) ivy"edu.berkeley.cs::chiseltest:0.6.0" else
      ivy"edu.berkeley.cs::chiseltest:6.0.0"
    )
  }
  def repositoriesTask = T.task { Seq(
    coursier.MavenRepository("https://repo.scala-sbt.org/scalasbt/maven-releases"),
    coursier.MavenRepository("https://oss.sonatype.org/content/repositories/releases"),
    coursier.MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
  ) ++ super.repositoriesTask() }
}
