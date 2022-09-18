import sbt._
import sbt.Keys._

object Akka {
  val AkkaVersion = "2.6.17"
  val ScalaTestVersion = "3.2.13"
  //  val silencerVersion = "1.4.2
  lazy val akkaHttpVersion = "10.2.9"
  lazy val akkaGrpcVersion = "2.1.4"

  val akkaSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
//      compilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerVersion),
//      "com.github.ghik" %% "silencer-lib" % silencerVersion
      // The Akka HTTP overwrites are required because Akka-gRPC depends on 10.1.x
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
    )
  )
}
