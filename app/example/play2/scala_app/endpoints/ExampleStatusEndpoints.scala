package example.play2.scala_app.endpoints

//// keep, commented out on purpose
//import app.example.play2.scala_app.monitoring.ExampleLocalCodeStatsD
//import com.codahale.metrics.{Meter, Metric, Timer}
//import play.api.libs.json._
//import play.api.mvc.{Action, Controller}
//
//// keep, commented out on purpose
//// import scala.collection.JavaConverters._
//// import com.codahale.metrics.{Meter, Metric, Timer}
//
//object ExampleStatusEndpoints extends Controller {
//
//  def exampleStatus = Action {
//    ExampleLocalCodeStatsD.increment("example.status")
//    Ok("Status: OK")
//  }
//
//  private def toJson: PartialFunction[Metric, JsObject] = {
//    case timer: Timer =>
//      val snapshot = timer.getSnapshot
//      Json.obj(
//        "max" -> snapshot.getMax,
//        "mean" -> snapshot.getMean,
//        "min" -> snapshot.getMin,
//        "rate" -> timer.getOneMinuteRate
//      )
//    case meter: Meter =>
//      Json.obj(
//      "rate" -> meter.getOneMinuteRate
//    )
//  }
//
//  def getMetrics = Action {
//    Ok {
//      JsObject {
//        global.getMetrics.asScala.toSeq sortBy {
//          case (exampleMetricName, _) => exampleMetricName
//        } collect {
//          case (exampleMetricName, exampleMetric) => exampleMetricName -> toJson(exampleMetric)
//        }
//      }
//    }
//  }
//}
