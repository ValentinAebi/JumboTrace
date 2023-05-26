ThisBuild / organization := "com.epfl.systemf.jumbotrace"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.2"
ThisBuild / scalacOptions += "-deprecation"

val asmVersion = "9.5"
val javaParserVersion = "3.25.3"
val playVersion = "2.9.4"

lazy val instrumenter = project
  .settings(
    name := "instrumenter",
    idePackagePrefix := Some("instrumenter"),
    libraryDependencies += "org.ow2.asm" % "asm" % asmVersion
  )

lazy val traceElements = project
  .settings(
    name := "traceElements",
    scalaVersion := "2.13.10",
    idePackagePrefix := Some("traceElements"),
    libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion
  )

lazy val debugCmdlineFrontend = project
  .settings(
    name := "debugCmdlineFrontend",
    idePackagePrefix := Some("debugCmdlineFrontend")
  ).dependsOn(traceElements)

lazy val javaCmdlineFrontend = project
  .settings(
    name := "javaCmdlineFrontend",
    idePackagePrefix := Some("javacmdfrontend"),
    libraryDependencies += "com.github.javaparser" % "javaparser-symbol-solver-core" % javaParserVersion,
  ).dependsOn(traceElements)
