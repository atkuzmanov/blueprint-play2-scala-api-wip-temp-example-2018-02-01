
import appexample.play2.scala_app.endpoints.helpers.ExampleHttpHelper
import example.play2.scala_app.ExampleEnviroConf
import example.play2.scala_app.ExampleEnviroConf.ExampleAWSCloudWatchConfObj
import example.play2.scala_app.endpoints.ExampleCustomHTTPHeaderException
import example.play2.scala_app.monitoring.ExampleLocalCodeStatsD
import example.play2.scala_app.services.AbstractObjectProcessingException
import filters.{ExampleFilterForLogging, ExampleWhitelistSieve}
import play.api.libs.json.JsObject

// import scala.tools.nsc.reporters.Reporter

import scala.tools.nsc.reporters.Reporter

// Need to implement these:
// import example.awscloudwatch.Metrics.Implicits.global //com.codahale.metrics.MetricRegistry
// import example.awscloudwatch.Reporter

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import play.api._
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{RequestHeader, Result, WithFilters}

import scala.concurrent._
import scala.language.postfixOps
import scala.sys.process._
import scala.util.control.NonFatal

object Global extends WithFilters(ExampleFilterForLogging, new ExampleWhitelistSieve) {
  private val logger: Logger = Logger(this.getClass)

  private val cloudWatchReporter: AtomicReference[Reporter] = new AtomicReference[Reporter]()
  private val NOT_FOUND_JSON_MSG: JsObject = Json.obj("status" -> "404", "message" -> "Not found.")
  private val shouldErrorsBeShownToClients: Boolean = ExampleEnviroConf.targetedEnviro == "dev" || ExampleEnviroConf.targetedEnviro == "int"

  override def onError(request: RequestHeader, exception: Throwable): Future[Result] = {
    exception.getCause match {
      case exception: AbstractObjectProcessingException => Future.successful(BadRequest(exception.getMessage))
      case exception: ExampleCustomHTTPHeaderException => Future.successful(BadRequest(exception.getMessage))
      case _ => if (shouldErrorsBeShownToClients) {
        super.onError(request, exception)
      } else {
//        ExampleLocalCodeStatsD.increment("error.500")
        increaseStatsOnError(request, 500)
        Future.successful(InternalServerError(Json.obj("statusCode" -> 500, "exceptionMsg" -> exception.getCause.getMessage)))
      }
    }
  }

  override def onStart(app: Application): Unit = {
    super.onStart(app)

    val currentEnviro: String = ExampleEnviroConf.exampleExecutionEnvironment

    logger.info(s"Adding public key for Scalegrid to key store for environment: [$currentEnviro].")

    val keyToolShellCommand: String = s"keytool -noprompt -import -file /usr/lib/example/path/to/example-public-key$currentEnviro.pub -alias scalegrid-pub-key-$currentEnviro -keystore /etc/pki/example/path/to/keyStore.jks -storepass examplePassword"
    val pubKeyAddResult: Int = keyToolShellCommand !

    pubKeyAddResult match {
      case 0 => logger.info("Scalegrid public key added successfully.")
      case errorCode => logger.error(s"Error on adding Scalegrid public key, error code = [$errorCode].")
    }

    if (ExampleAWSCloudWatchConfObj.enabled) {
      logger.info("Initiating CloudWatch reporter... ")
      try {
        val cloudWatchClient: AmazonCloudWatchClient = new AmazonCloudWatchClient {
          setRegion(Region getRegion Regions.EU_WEST_1)
        }
        cloudWatchClientConnect(cloudWatchClient)
      } catch {
        case NonFatal(ex) => logger.error("Error on starting CloudWatchReporter.", ex)
      }
    }
//    ExampleLocalCodeStatsD.increment("application.start")
  }

  override def onHandlerNotFound(requestHeader: RequestHeader): Future[Result] = {
//    ExampleLocalCodeStatsD.increment("error.404")
    increaseStatsOnError(requestHeader, 404)
    Future.successful(NotFound(NOT_FOUND_JSON_MSG))
  }

  override def onBadRequest(requestHeader: RequestHeader, badRequestError: String): Future[Result] = {
//    ExampleLocalCodeStatsD.increment("error.400")
    increaseStatsOnError(requestHeader, 400)
    Future.successful(BadRequest(badRequestError))
  }

  override def onStop(app: Application): Unit = {
//    ExampleLocalCodeStatsD.increment("application.stop")
    Option(cloudWatchReporter.get) foreach (_.stop())
    super.onStop(app)
  }

  private def increaseStatsOnError(requestHeader: RequestHeader, httpStatus: Int): Unit = {
    val key: String = s"exampleEndpoint.statusCode.${ExampleHttpHelper.endpointFinder(requestHeader.path)}.$httpStatus"
    logger.debug(s"increaseStatsOnError: $key")
//    ExampleLocalCodeStatsD.increment(key)
  }

  private def cloudWatchClientConnect(awsCloudWatchClient: AmazonCloudWatchClient): Unit = {
    val reporter: Reporter = new Reporter(global, "pla2-scala-example-api-app-name", awsCloudWatchClient, ExampleEnviroConf.enviro)
    cloudWatchReporter.set(reporter)
    cloudWatchReporter.get.start(1, TimeUnit.MINUTES)
    logger.info("Started CloudWatch reporter.")
  }
}

