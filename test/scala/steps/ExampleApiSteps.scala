package steps

import example.play2.scala_app.ExampleEnviroConf._
import org.scalatest.MustMatchers
import org.apache.commons.lang3.StringUtils

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import play.api.libs.ws.WSResponse
import cucumber.api.scala.{EN, ScalaDsl}
import _root_.util.ExampleAPITestServer
import example.play2.scala_app.endpoints.ExampleCustomHeaders

trait ExampleApiSteps extends ExampleTestEnviroConf with ScalaDsl with EN with MustMatchers {

  protected val exampleResponseTimeout: FiniteDuration = 35.seconds

  def exampleWSResponse_=(response: WSResponse): Unit = {
    ExampleResponseHolderObject.response = response
  }
  def exampleWSResponse: WSResponse = ExampleResponseHolderObject.response

  def exampleRespLocation: Option[String] = exampleWSResponse.header("Location")
  def exampleRespContentType: Option[String] = exampleWSResponse.header("Content-Type")

  val exampleApplicationJsonHeader: (String, String) = "Content-type" -> "application/json"
  val exampleUsernameHeader: (String) => (String, String) = {
    (exampleUsername: String) => ExampleCustomHeaders.ExampleCustomHTTPHeader1 -> exampleUsername
  }
  val exampleApplicationJsonAcceptHeader: (String, String) = "Accept" -> "application/json"

  def exampleExtractStringFromPayload(target: String, payload: String): String = {
    val exampleCountKeyWordMatches: Int = StringUtils.countMatches(payload, "example-search-key-word")
    if (exampleCountKeyWordMatches == 1) return payload
    val exampleStringInBetween: String = StringUtils.substringBetween(payload, s"example-$target", "example-search-key-word")
    if (StringUtils.isNotEmpty(exampleStringInBetween)) exampleStringInBetween else StringUtils.substringAfterLast(payload, s"example-$target")
  }

  def exampleAwaitResponseFuture(future: Future[WSResponse]): Unit = {
    exampleWSResponse = Await.result(future, exampleResponseTimeout)
  }

  ExampleAPITestServer.startTestServer
}

object ExampleDefaultMockServer {
  val exampleMockServer: MockServer = MockServer(9096)
}

trait ExampleTestEnviroConf {
  val exampleFoundationURI: String = exampleExecutionEnvironment match {
    case "management-jenkins" => "http://localhost:5151"
    case "management-acceptance-tests" => "https://example.api.integration.com"
    case _ => "http://localhost:5151"
  }
}

object ExampleResponseHolderObject { var response: WSResponse = _ }
