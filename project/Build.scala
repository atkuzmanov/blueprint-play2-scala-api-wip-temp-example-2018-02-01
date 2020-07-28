import sbt.Keys._
import sbt._
import sbt.{Build => SbtBuild}
import sbtassembly.Plugin._
import sbtassembly.Plugin.AssemblyKeys._
import templemore.sbt.cucumber.CucumberPlugin
import scoverage.ScoverageKeys._
import play.Play.autoImport._

import scala.util.matching.Regex

object Build extends SbtBuild {
  val PlayVersion: String = "2.3.10"
  val testCoverageExcludedFiles: String = "<empty>;Reverse.*;.*package.*"
  val name: String = "example-play2-scala_app"

  val apiDependencies: Seq[ModuleID] = {
    Seq(
      // runtime dependencies
      "com.amazonaws" % "aws-java-sdk" % "1.10.60",
      "com.typesafe.play" % "play-ws_2.10" % PlayVersion,
      "com.typesafe.play" % "play-json_2.10" % PlayVersion,
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
      "org.apache.commons" % "commons-lang3" % "3.3.2",
      "com.github.fge" % "json-schema-validator" % "2.2.6",
      "org.mongodb" % "mongo-java-driver" % "2.14.2",
      "com.codahale.metrics" % "metrics-core" % "3.0.1",

      // test dependencies
      "org.mockito" % "mockito-core" % "1.9.5" % "test,it",
      "org.scalatest" %% "scalatest" % "2.2.5" % "test,it",
      "info.cukes" %% "cucumber-scala" % "1.2.4" % "test",
      "com.github.tomakehurst" % "wiremock" % "1.46" % "test"
    )
  }

  val playPattern: Regex = "(play/core/server/.*)".r
  val loggingPattern: Regex = "(org/apache/commons/logging/.*)".r
  val apiVersion: String = Option(System.getenv("BUILD_VERSION")) getOrElse "DEV"

//  libraryDependencies += "com.codahale" % "jerkson_2.8.2" % "0.5.0"

  val main: Project = Project("example-play2-scala_app", file("."))
    .enablePlugins(play.PlayScala)
    .configs(IntegrationTest)
    .settings(version := apiVersion, libraryDependencies ++= apiDependencies)
    .settings(assemblySettings: _*)
    .settings(
      jarName in assembly := s"$name.jar",
      mainClass in assembly := Some("play.core.server.NettyServer"),
      fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
      mergeStrategy in assembly := {
        case loggingPattern(_) | playPattern(_) => MergeStrategy.first
        case x =>
          val oldStrategy = (mergeStrategy in assembly).value
          oldStrategy(x)
      }
    )
    .settings(resolvers += "Example Maven Repository Releases" at "https://mvnrepository.com/")
    .settings(Defaults.itSettings: _*)
    .settings(scalacOptions ++= Seq("-feature", "-target:jvm-1.7"))
    .settings(scalaSource in IntegrationTest <<= baseDirectory(_ / "it/scala"))
    .settings(
      unmanagedSourceDirectories in Test += baseDirectory.value / "test/scala",
      unmanagedResourceDirectories in Test <+= baseDirectory(_ / "test/resources")
    )
    .settings(CucumberPlugin.cucumberSettings: _*)
    .settings(
      coverageExcludedPackages := testCoverageExcludedFiles,
      coverageMinimum := 80,
      coverageFailOnMinimum := true)
    .settings(
      resolvers += "Local Ivy repository" at "file:///" + Path.userHome + "/.ivy2/local",
      CucumberPlugin.cucumberJsonReport := true,
      CucumberPlugin.cucumberFeaturesLocation := "cucumber",
      CucumberPlugin.cucumberStepsBasePackage := "steps",
      unmanagedResourceDirectories in Test <+= baseDirectory(_ / "test/fixtures")
    )
    .settings((test in Test) <<= (test in Test) dependsOn (test in IntegrationTest))
}
