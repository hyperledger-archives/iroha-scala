scalaVersion in ThisBuild := "2.12.8"
licenses in ThisBuild += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

lazy val `iroha-scala` = (project in file("."))
  .aggregate(`iroha-monix`)


val commonSettings: Seq[Setting[_]] = Seq(
  resolvers += "jitpack" at "https://jitpack.io",
  libraryDependencies += "com.github.warchant" % "ed25519-sha3-java" % "2.0.1"
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

lazy val `iroha-monix` = (project in file("monix"))
  .settings(
    name := "iroha-monix"
  )
  .settings(Monix.acyclicSettings: _*)
  .settings(Monix.compilerSettings: _*)
  .settings(Monix.testSettings: _*)
  .settings(Monix.grpcSettings: _*)
  .settings(Monix.monixSettings:_*)
  .settings(commonSettings:_*)
  .enablePlugins(ProtocPlugin)
