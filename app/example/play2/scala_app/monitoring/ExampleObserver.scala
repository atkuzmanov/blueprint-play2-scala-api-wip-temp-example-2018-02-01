package example.play2.scala_app.monitoring

// keep, commented out on purpose
//import com.codahale.metrics.Timer.Context
//import com.codahale.metrics.MetricRegistry
//import com.codahale.metrics.Timer

import java.util.Timer
import scala.concurrent.ExecutionContext.Implicits.{global => executionContext}
import scala.concurrent.{ExecutionContext, Future}

object ExampleObserver {
  // keep, commented out on purpose
  //  def exampleAsyncTiming[A](exampleTimer: Timer)(exampleBlockOfCode: => Future[A])(implicit exc: ExecutionContext): Future[A] = {
  //    val timeContext: Context = exampleTimer.time
  //    exampleBlockOfCode map { executionResult =>
  //      timeContext.stop()
  //      executionResult
  //    }
  //  }
  //
  //  def exampleAsyncTiming[A](exampleName: String)(exampleCodeBlock: => Future[A])(implicit exc: ExecutionContext, metricReg: MetricRegistry): Future[A] = {
  //    exampleAsyncTiming(metricReg.timer(exampleName))(exampleCodeBlock)
  //  }
  //
  //  def exampleObserveABlockOfCode[A](exampleAWSCloudWatchKey: String, exampleStatsDKey: String)(codeBlock: => Future[A]): Future[A] = {
  //    exampleAsyncTiming(exampleAWSCloudWatchKey) {
  //      ExampleLocalCodeStatsD.exampleAsyncTiming(exampleStatsDKey) {
  //        ExampleLocalCodeStatsD.increment(exampleStatsDKey)
  //        codeBlock
  //      }
  //    }
  //  }

  def exampleAsyncTiming[A](exampleTimer: Timer)(exampleBlockOfCode: => Future[A])(implicit exc: ExecutionContext): Future[A] = {
    ???
  }

  def exampleAsyncTiming[A](exampleName: String)(exampleCodeBlock: => Future[A])(implicit exc: ExecutionContext, metricReg: MetricRegistry): Future[A] = {
    ???
  }

  def exampleObserveABlockOfCode[A](exampleAWSCloudWatchKey: String, exampleStatsDKey: String)(codeBlock: => Future[A]): Future[A] = {
    ???
  }
}

