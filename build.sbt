scalaVersion := "2.13.12"
val useChisel6 = true

lazy val root = (project in file("."))
  .settings(
    name := "NoAXI-LoongArch-CPU",
    libraryDependencies ++= Seq(
      if (useChisel6) {
        "org.chipsalliance" %% "chisel" % "6.3.0"
      } else {
        "edu.berkeley.cs" %% "chisel3" % "3.5.4"
      },
    ),
    scalacOptions ++= Seq(
      "-language:reflectiveCalls",
      "-deprecation",
      "-feature",
      "-Xcheckinit",
      "-Yrangepos",
    ),
    addCompilerPlugin(
      if(useChisel6) {
        "org.chipsalliance" % "chisel-plugin" % "6.3.0" cross CrossVersion.full
      } else {
        "edu.berkeley.cs" % "chisel3-plugin" % "3.5.4" cross CrossVersion.full
      },
    ),
  )