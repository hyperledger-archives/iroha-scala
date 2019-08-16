scalaVersion in ThisBuild := "2.12.8"
licenses in ThisBuild += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))


val versions = new {
  val dottyVersion = "0.13.0-RC1"
  val scala212Version = "2.12.8"

  val acyclic      = "0.1.8"
  val utest        = "0.6.6"

  val scalapbc     = scalapb.compiler.Version.scalapbVersion
  val grpc         = scalapb.compiler.Version.grpcJavaVersion
  val ed25519      = "2.0.1"
  val monix        = "3.0.0-RC2"
}


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

val cryptoSettings: Seq[Setting[_]] =
  Seq(
    libraryDependencies ++=
      Seq(
        "jp.co.soramitsu.crypto"            %  "ed25519" % "2.0.2-SNAPSHOT", 
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

val testSettings: Seq[Setting[_]] =
  Seq(
    libraryDependencies += "com.lihaoyi" %% "utest" % versions.utest % "test",
    fork in Test := true,
    testFrameworks += TestFramework("utest.runner.Framework")
  )

val publishSettings: Seq[Setting[_]] =
  Seq(
    publishMavenStyle := false,
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    //FIXME useGpg in xxxGlobalScope := true
    //TODO: bintrayRepository := "iroha-scala",
    //TODO: bintrayOrganization in bintray := None,
  )



lazy val root =
  (project in file("."))
    .aggregate(core)


lazy val core = (project in file("."))
  .settings(
    name := "iroha-scala",
  )
  .settings(acyclicSettings: _*)
  .settings(compilerSettings: _*)
  .settings(testSettings: _*)
  .settings(grpcSettings: _*)
  .settings(cryptoSettings:_*)
  .settings(monixSettings:_*)
  .enablePlugins(ProtocPlugin)
