package test.scala.example.play2.scala_app.persistence

import example.play2.scala_app.integration.ExampleDataAccess
import example.play2.scala_app.persistence.ExampleAbstractObjectDAO
import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mock.MockitoSugar

import scala.concurrent.Future

trait ExampleMockDAOs extends MockitoSugar {
  val abstrObjsDAO: ExampleAbstractObjectDAO = mock[ExampleAbstractObjectDAO]

  def resetMocks() = {
    Mockito.reset(abstrObjsDAO)
  }

  object MockDataAccess extends ExampleDataAccess {
    val abstrObjsDAO = ExampleMockDAOs.this.abstrObjsDAO
  }

  implicit class FutureReturn[T](ongoing: OngoingStubbing[Future[T]]) {
    def thenReturnFuture(foo: T): OngoingStubbing[Future[T]] = ongoing thenReturn Future.successful(foo)
  }
}


