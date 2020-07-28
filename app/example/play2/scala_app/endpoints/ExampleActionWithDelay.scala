package example.play2.scala_app.endpoints

import akka.actor.ActorSystem

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.mvc.{ActionBuilder, Request, Result, WrappedRequest}
import play.api.libs.concurrent.Akka
import play.api.Play.current
import org.joda.time.{DateTime, DateTimeZone}

class ExampleRequestWithTimestamp[A](request: Request[A], val timestamp: Long) extends WrappedRequest[A](request)

object ExampleActionWithDelay {
  private val system: ActorSystem = Akka.system
  def apply(durationOfDelay: FiniteDuration): ExampleActionWithDelay = new ExampleActionWithDelay(durationOfDelay)
}

class ExampleActionWithDelay(durationOfDelay: FiniteDuration) extends ActionBuilder[ExampleRequestWithTimestamp] {
  private val defaultTimestamp: Long = DateTime.now(DateTimeZone.UTC).getMillis
  def invokeBlock[A](defaultRequest: Request[A], executionBlock: (ExampleRequestWithTimestamp[A]) => Future[Result]): Future[Result] = {
    akka.pattern.after(durationOfDelay, using = ExampleActionWithDelay.system.scheduler) {
      executionBlock(new ExampleRequestWithTimestamp(defaultRequest, defaultTimestamp))
    }
  }
}
