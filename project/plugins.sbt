// Comment out the line below (logLevel := Level.Warn) to get more information during initialization, or change logging level.
logLevel := Level.Warn

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

resolvers += "Templemore Repository" at "http://templemore.co.uk/repo"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Local Ivy repository" at "file:///" + Path.userHome + "/.ivy2/local"

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.5.1")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.10")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.5")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.9")

addSbtPlugin("templemore" % "sbt-cucumber-plugin" % "0.8.0")
