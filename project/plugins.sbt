resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.1")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.45"
