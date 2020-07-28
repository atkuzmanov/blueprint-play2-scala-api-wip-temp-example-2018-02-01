package util

import java.util

import scala.collection.JavaConverters._
import play.api.test.TestServer

import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.client.{RequestPatternBuilder, UrlMatchingStrategy}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.verification.LoggedRequest

object ExampleAPITestServer {
  private var exampleTestServer: Option[TestServer] = None
  private val defaultTestServerPort: Int = 5151

  def startTestServer: TestServer = {
    exampleTestServer match {
      case Some(_) => exampleTestServer.get
      case None =>
        exampleTestServer = Some(TestServer(defaultTestServerPort))
        println(s"<<< Starting Test Server on port: [$defaultTestServerPort].")
        exampleTestServer.get.start()
        exampleTestServer.get
    }
  }

  def exampleGetAllPayloadsOfCallTo(urlMatchStrategy: UrlMatchingStrategy): Seq[String] = {
    val exampleReqPatternBuilder = new RequestPatternBuilder(RequestMethod.ANY, urlMatchStrategy)
    val exampleAllRequestsFound: util.List[LoggedRequest] = findAll(exampleReqPatternBuilder)
    if (exampleAllRequestsFound.size() != 0)
      exampleAllRequestsFound.iterator().asScala.toList map { loggedRequest =>
        loggedRequest.getBodyAsString
      }
    else
      Seq.empty
  }
}

