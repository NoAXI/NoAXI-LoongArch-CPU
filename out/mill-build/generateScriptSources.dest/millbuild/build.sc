package millbuild

import _root_.mill.runner.MillBuildRootModule

object MiscInfo_build {
  implicit lazy val millBuildRootModuleInfo: _root_.mill.runner.MillBuildRootModule.Info = _root_.mill.runner.MillBuildRootModule.Info(
    Vector("/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunjce_provider.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/zipfs.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/cldrdata.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/icedtea-sound.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunec.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/dnsns.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/nashorn.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/java-atk-wrapper.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/sunpkcs11.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/jaccess.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/libatk-wrapper.so", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/ext/localedata.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/resources.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jsse.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jce.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/charsets.jar", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/jfr.jar", "/home/yuxuaan/Chisel_CPU_Learning/LA64CPU/out/mill-launcher/0.11.7.jar").map(_root_.os.Path(_)),
    _root_.os.Path("/home/yuxuaan/Chisel_CPU_Learning/LA64CPU"),
    _root_.os.Path("/home/yuxuaan/Chisel_CPU_Learning/LA64CPU"),
  )
  implicit lazy val millBaseModuleInfo: _root_.mill.main.RootModule.Info = _root_.mill.main.RootModule.Info(
    millBuildRootModuleInfo.projectRoot,
    _root_.mill.define.Discover[build]
  )
}
import MiscInfo_build.{millBuildRootModuleInfo, millBaseModuleInfo}
object build extends build
class build extends _root_.mill.main.RootModule {

//MILL_ORIGINAL_FILE_PATH=/home/yuxuaan/Chisel_CPU_Learning/LA64CPU/build.sc
//MILL_USER_CODE_START_MARKER
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

}