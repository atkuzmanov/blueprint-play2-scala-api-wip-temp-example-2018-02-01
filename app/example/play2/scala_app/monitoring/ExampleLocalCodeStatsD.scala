package example.play2.scala_app.monitoring

import example.play2.scala_app.ExampleEnviroConf
import example.play2.scala_app.ExampleEnviroConf

// Needs to be implemented:
// import example.play2.monitoring.{Monitoring, StatsD}

import play.api.libs.concurrent.Akka
import play.api.Play.current

// keep, commented out on purpose
object ExampleLocalCodeStatsD {
//  val exampleStatsD: ExampleLocalCodeStatsD = new example.play2.scala_app.monitoring.StatsD(actorSystem = Akka.system)
  def getStatsD = ExampleLocalCodeStatsD
  def environment = ExampleEnviroConf.targetedEnviro
  def componentName = "pla2-scala-example-api-app-name"
}


