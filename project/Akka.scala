import sbt._
import sbt.Keys._

object Akka {
  val AkkaVersion = "2.5.19"
  val ScalaTestVersion = "3.0.4"
  
  val akkaSettings: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % Test,
    )
  )
}
