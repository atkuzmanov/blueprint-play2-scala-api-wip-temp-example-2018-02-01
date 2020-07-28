package test.scala.example.play2.scala_app.services

import app.example.play2.scala_app.Identifier
import app.example.play2.scala_app.domain.ExampleAbstractObjectTransmutators
import app.example.play2.scala_app.persistence.ExampleAbstractObjectDAO
import app.example.play2.scala_app.services.helpers.ExampleSystemDateTime
import app.example.play2.scala_app.services.{ExampleAbstractObjService, ExamplePublishToAWSSNSTopic, ExampleUnimplementedAPIHitter}
import example.play2.scala_app.Identifier
import example.play2.scala_app.domain.ExampleAbstractObjectTransmutators
import example.play2.scala_app.persistence.ExampleAbstractObjectDAO
import example.play2.scala_app.services.helpers.SystemDateTime
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.invocation.InvocationOnMock
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.stubbing.{Answer, OngoingStubbing}
import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.{BeforeAndAfter, FlatSpec, MustMatchers}
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class ExampleAbstrObjServiceTestSpec extends FlatSpec with MustMatchers with MockitoSugar with BeforeAndAfter with PatienceConfiguration {

  import play.api.libs.json.Json._

  implicit class ThrowCheckedStub[T](val stub: OngoingStubbing[T]) {
    def thenThrowChecked(e: Throwable) = stub thenAnswer new Answer[T] {
      def answer(invoke: InvocationOnMock) = throw e
    }
  }

  val mockExampleMockJsonObj: JsObject = mock[JsObject]
  val mockExampleAbstrObjDAO: ExampleAbstractObjectDAO = mock[ExampleAbstractObjectDAO]
  val mockExampleDateTime: ExampleSystemDateTime = mock[ExampleSystemDateTime]
  val mockExamplePublishToAWSSNSTopic: ExamplePublishToAWSSNSTopic = mock[ExamplePublishToAWSSNSTopic]
  val mockExampleUnimplAPIHit: ExampleUnimplementedAPIHitter = mock[ExampleUnimplementedAPIHitter]
  val mockExampleAbstrObjTransmut: ExampleAbstractObjectTransmutators = mock[ExampleAbstractObjectTransmutators]

  before {
    reset(mockExampleAbstrObjDAO, mockExampleDateTime, mockExampleMockJsonObj, mockExampleUnimplAPIHit, mockExamplePublishToAWSSNSTopic, mockExampleAbstrObjTransmut)
  }

  val exampleIdentifier: Identifier = "123asd456qwerty"
  val exampleTimestamp: Long = 1499351946L
  val exampleUser: String = "example_user"
  val exampleDateTime: DateTime = new DateTime(exampleTimestamp, DateTimeZone.UTC)
  val exampleAbstrObjService = new ExampleAbstractObjService(mockExampleAbstrObjTransmut, mockExampleAbstrObjDAO, mockExampleDateTime, mockExampleUnimplAPIHit, mockExamplePublishToAWSSNSTopic)

  "Some example function call" should "fail if an abstract obj cannot be stored" in {
    val exampleTestAbstrObj: JsObject = obj(
      "example_element_1" -> "example_invalid_content",
      "example_identifier" -> JsNumber(1234567),
      "meta" -> obj(
        "state" -> JsString("new")
      )
    )

    when(mockExampleAbstrObjTransmut.defaultConstructCreatedAbstrObj(exampleDateTime, exampleTestAbstrObj, Some("123456"), Some("78910"),exampleUser, exampleIdentifier)) thenReturn obj()
    when(mockExampleDateTime.now) thenReturn exampleDateTime
    when(mockExampleAbstrObjDAO.createAbstrObj(any[JsObject])) thenReturn Future.failed(new Exception("Example forced mock exception error!"))

    val exampleFutureAbstrObj: Future[JsObject] = exampleAbstrObjService.saveNewAbstrObj("123456", exampleTestAbstrObj, exampleUser)

    ScalaFutures.whenReady(exampleFutureAbstrObj.failed) { e =>
      e.getMessage mustBe "Example forced mock exception error!"
      e.getClass mustBe classOf[Exception]
    }

    verify(mockExampleAbstrObjTransmut).defaultConstructCreatedAbstrObj(exampleDateTime, exampleTestAbstrObj, Some("1234"), Some("5678"), exampleUser, exampleIdentifier)
    verify(mockExampleAbstrObjDAO).createAbstrObj(any[JsObject])
    verify(mockExampleDateTime).now
  }

  it should "call the postDao with the post JSON" in {
    val inputPost = obj(
      "example_element_1" -> "example_content",
      "meta" -> obj(
        "exampleCreatedTimestamp" -> JsNumber(exampleTimestamp),
        "exampleUser" -> JsString(exampleUser),
        "exampleLastUpdatedTimestamp" -> JsNumber(exampleTimestamp),
        "state" -> JsString("new"),
      ),
      "example_identifier" -> exampleIdentifier
    )
    when(mockExampleAbstrObjDAO.saveAbstrObj(inputPost)) thenReturn Future.successful()
    Await.result(exampleAbstrObjService.saveAbstrObj(inputPost), 3.seconds)
    verify(mockExampleAbstrObjDAO).saveAbstrObj(inputPost)
  }
}

