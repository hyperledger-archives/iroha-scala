import sbt._
import sbt.Keys._

object Akka {
  val AkkaVersion = "2.5.24"
  val ScalaTestVersion = "3.0.4"
  val silencerVersion = "1.4.2"
  
  val akkaSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion % Test,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % silencerVersion),
      "com.github.ghik" %% "silencer-lib" % silencerVersion
    )
  )
}
