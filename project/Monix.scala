import sbt._
import sbt.Keys._
import sbtprotoc.ProtocPlugin.autoImport.PB

object Monix {
  val versions = new {
    val dottyVersion = "0.13.0-RC1"
    val scala212Version = "2.12.8"

    val acyclic      = "0.1.8"
    val utest        = "0.6.6"

    val scalapbc     = scalapb.compiler.Version.scalapbVersion
    val grpc         = scalapb.compiler.Version.grpcJavaVersion
    val monix        = "3.0.0-RC2"
  }

  val testSettings: Seq[Setting[_]] =
    Seq(
      libraryDependencies += "com.lihaoyi" %% "utest" % versions.utest % "test",
      fork in Test := true,
      testFrameworks += TestFramework("utest.runner.Framework")
    )

  val grpcSettings: Seq[Setting[_]] =
    Seq(
      libraryDependencies ++=
        Seq(
          "io.grpc"                           %  "grpc-netty"           % versions.grpc,
          "com.thesamet.scalapb"              %% "scalapb-runtime-grpc" % versions.scalapbc,
          "com.thesamet.scalapb"              %% "scalapb-runtime-grpc" % versions.scalapbc % "protobuf",
        ),
      PB.targets in Compile :=
        Seq(
          scalapb.gen() -> (sourceManaged in Compile).value,
        )
    )

  val monixSettings: Seq[Setting[_]] =
    Seq(
      libraryDependencies += "io.monix" %% "monix" % versions.monix,
    )

  val acyclicSettings: Seq[Setting[_]] =
    Seq(
      libraryDependencies += "com.lihaoyi" %% "acyclic" % versions.acyclic % "provided",
      autoCompilerPlugins  := true,
      addCompilerPlugin("com.lihaoyi" %% "acyclic" % versions.acyclic),
      scalacOptions ++=
        Seq(
          "-feature",
          //XXX "-P:acyclic:force",
          "-Ypartial-unification"))

  val compilerSettings: Seq[Setting[_]] =
    Seq(
      scalaVersion := versions.scala212Version,
      crossScalaVersions := Seq(versions.dottyVersion, versions.scala212Version),
      javacOptions ++= Seq(
        "-source", "1.8",
        "-target", "1.8",
        "-encoding", "UTF-8",
      ),
      scalacOptions ++= Seq(
        "-target:jvm-1.8",
        "-encoding", "UTF-8",
        "-unchecked",
        "-deprecation",
        //XXX "-Xfuture",
        //XXX "-Yno-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-numeric-widen",
        "-Ywarn-value-discard",
        "-Ywarn-unused",
      ),
    )
}
