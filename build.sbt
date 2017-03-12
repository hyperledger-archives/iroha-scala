name := "iroha-scala"

version := "1.0.0"

val PROJECT_SCALA_VERSION = "2.11.8"

scalaVersion := PROJECT_SCALA_VERSION

useGpg in GlobalScope := true

lazy val librairies = Seq(
  "io.grpc" % "grpc-netty" % "1.0.1",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion,
  "com.trueaccord.scalapb" %% "scalapb-runtime" % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf",
  "org.bouncycastle" % "bcpg-jdk15on" % "1.51",
  "net.i2p.crypto" % "eddsa" % "0.1.0",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

lazy val settings = Seq(
  organization := "net.cimadai",
  scalaVersion := PROJECT_SCALA_VERSION,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.7", "-encoding", "UTF-8"),
  javaOptions ++= Seq("-Xmx1G"),
  scalacOptions ++= Seq(
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-unchecked",
    "-deprecation",
    "-Xfuture",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused"
  ),
  libraryDependencies ++= librairies,

  fork in Test := true,

  publishMavenStyle := true,

  publishArtifact in Test := false,

  pomIncludeRepository := { _ => false },

  publishTo <<= version { (v: String) =>
    val nexus = "https://oss.sonatype.org/"
    if (v.trim.endsWith("SNAPSHOT"))
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },

  pomExtra := <url>https://github.com/cimadai/chatwork-scala</url>
    <licenses>
      <license>
        <name>The MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:cimadai/iroha-scala.git</url>
      <connection>scm:git:git@github.com:cimadai/iroha-scala.git</connection>
    </scm>
    <developers>
      <developer>
        <id>cimadai</id>
        <name>Daisuke Shimada</name>
        <url>https://github.com/cimadai</url>
      </developer>
    </developers>
)

lazy val irohaScala = (project in file("."))
  .enablePlugins(ProtocPlugin)
  .settings(settings: _*)
  .settings(name := "chatwork-scala")
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    PB.protoSources in Compile := Seq(file("protos"))
  )

