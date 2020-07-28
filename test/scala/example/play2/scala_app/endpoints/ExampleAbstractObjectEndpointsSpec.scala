package test.scala.example.play2.scala_app.endpoints

import example.play2.scala_app.services._
import _root_.util.ExampleTestUtilities
import com.codahale.metrics.MetricRegistry
import example.play2.scala_app.endpoints.{ExampleAbstractObjectEndpoints, ExampleCustomHeaders}

import scala.concurrent.Future
import play.api.test.Helpers._
import play.api.libs.json.Json._
import play.api.libs.iteratee.Iteratee
import play.api.mvc.{AnyContentAsEmpty, Controller, Result}
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.libs.json._
import org.scalatest.{BeforeAndAfter, FlatSpec, MustMatchers}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

class ExampleAbstractObjectEndpointsSpec extends FlatSpec with MustMatchers with MockitoSugar with BeforeAndAfter with ExampleTestUtilities {
  // Need to implement: example.default.cloudwatch.Metrics.Implicits.global
  // val metricRegistrySpy = spy(example.default.cloudwatch.Metrics.Implicits.global)
  val metricRegistrySpy = spy(scala.concurrent.ExecutionContext.Implicits.global)
  val mockExampleAbstrObjService = mock[ExampleAbstractObjService]

  class TestAbstrObjs extends Controller with ExampleAbstractObjectEndpoints {
    // val exampleMetricRegistry: MetricRegistry = metricRegistrySpy
    // val exampleAbstrObjService: ExampleAbstractObjService = mockExampleAbstrObjService
    override protected val exampleAbstrObjService: ExampleAbstractObjService = mockExampleAbstrObjService
    override protected implicit val exampleMetricRegistry: MetricRegistry = metricRegistrySpy
  }

  val exampleController: TestAbstrObjs = new TestAbstrObjs
  val exampldeId = "1234asdf56789qwerty"
  val exampleUser = "example_default_username"
  val exampleTimestamp = 1499343612L
  val exampleAbstObjJS: JsObject = Json.obj(
    "example_id" -> exampldeId,
    "example_element1" -> "Qwerty",
    "example_metadata" -> Json.obj(
      "example_createdTimestamp" -> exampleTimestamp,
      "example_user" -> exampleUser,
      "example_state" -> "new"),
    "example_element2" -> "Abcdefg")
  val examplePhonyHeaders: (String) => FakeHeaders = {
    (exampleUser: String) =>
      FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"), ExampleCustomHeaders.ExampleCustomHTTPHeader1 -> Seq(exampleUser)))
  }

  before {
    reset(mockExampleAbstrObjService)
  }

  "GET /abstractObjects/:abstrObjIdentifier" should "return 200 response and an existing abstract object" in {
    when(mockExampleAbstrObjService.getByAbstrObjIdentifier(exampldeId)) thenReturn Future.successful(Some(exampleAbstObjJS))
    val exampleRequestPayload: String = "{}"
    val examplePhonyRequest: FakeRequest[String] = FakeRequest(GET, routes.ExampleAbstractObjectEndpoints.exampleRetrieveByAbstrObjId(exampldeId).url, examplePhonyHeaders(exampleUser), exampleRequestPayload)
    val exampleTestResult: Iteratee[Array[Byte], Result] = exampleController.exampleRetrieveByAbstrObjId(exampldeId)(examplePhonyRequest)
    contentAsJson(exampleTestResult) mustEqual exampleAbstObjJS
    contentType(exampleTestResult) mustBe Some("application/json")
    status(exampleTestResult) mustBe OK
    verify(mockExampleAbstrObjService).getByAbstrObjIdentifier(exampldeId)
  }

  it should "return a 404 Not Found response for non-existing abstract object" in {
    when(mockExampleAbstrObjService.getByAbstrObjIdentifier(exampldeId)) thenReturn Future.successful(None)
    val examplePhonyRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.ExampleAbstractObjectEndpoints.exampleRetrieveByAbstrObjId(exampldeId).url)
    val exampleTestResult: Future[Result] = exampleController.exampleRetrieveByAbstrObjId(exampldeId)(examplePhonyRequest)
    contentAsString(exampleTestResult) mustBe ""
    status(exampleTestResult) mustBe NOT_FOUND
    verify(mockExampleAbstrObjService).getByAbstrObjIdentifier(exampldeId)
  }
}

