scalaVersion in ThisBuild := "2.13.8"
licenses in ThisBuild += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

val scalapbc = scalapb.compiler.Version.scalapbVersion

skip in publish := true

lazy val `iroha-scala` = (project in file("."))
//  .aggregate(`iroha-monix`)
  .aggregate(`iroha-akka`)


val commonSettings: Seq[Setting[_]] = Seq(
  resolvers += "jitpack" at "https://jitpack.io",
  libraryDependencies ++= Seq(
    "com.github.warchant" % "ed25519-sha3-java" % "2.0.1",
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbc,
    "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbc % "protobuf",
  )
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

//lazy val `iroha-monix` = (project in file("monix"))
//  .settings(
//    name := "iroha-monix"
//  )
//  .settings(Monix.acyclicSettings: _*)
//  .settings(Monix.compilerSettings: _*)
//  .settings(Monix.testSettings: _*)
//  .settings(Monix.grpcSettings: _*)
//  .settings(Monix.monixSettings:_*)
//  .settings(commonSettings: _*)
//  .enablePlugins(ProtocPlugin)

lazy val `iroha-akka` = (project in file("akka"))
  .settings(
    credentials += Credentials(Path.userHome / ".sbt" / "github-maven.credentials"),
    resolvers += "castleone-github" at "https://maven.pkg.github.com/CastleOne",
    name := "iroha-akka",
    organization := "castleone",
    fork in Test := true,
    publishMavenStyle := true,
    publishArtifact in Test := false,
    skip in publish := false,
    sources in (Compile,doc) := Seq.empty,
    publishArtifact in (Compile, packageDoc) := false,
    publishTo := Option("github" at "https://maven.pkg.github.com/CastleOne/iroha-scala"),
  )
  .settings(Akka.akkaSettings: _*)
  .settings(commonSettings: _*)
  .settings(
    scalacOptions ++= Seq(
      //Set a relative file path
//      s"-P:silencer:sourceRoots=${baseDirectory.value.getCanonicalPath}",
//      //Ignore the google protobuf errors. https://github.com/akka/akka-grpc/issues/540.
//      "-P:silencer:pathFilters=target/scala-2.12/src_managed/main/com/google/protobuf/descriptor/FileOptions.scala",
      "-unchecked", "-deprecation", "-feature", "-Xfatal-warnings"
    )
  )
  .enablePlugins(AkkaGrpcPlugin)
