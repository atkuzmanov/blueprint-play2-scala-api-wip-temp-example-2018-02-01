package filters

import java.security.cert.Certificate

import app.example.play2.scala_app.ExampleEnviroConf
import example.scala.whitelist.ExampleWhitelist
import example.play2.scala_app.ExampleEnviroConf
import play.api.mvc.{Filter, RequestHeader, Result}
import play.api.mvc.Results._

import scala.concurrent.Future
import org.slf4j.LoggerFactory

class ExampleWhitelistSieve(environment: String = ExampleEnviroConf.enviro, whitelist: Whitelist = new Whitelist(ExampleEnviroConf.ExampleWhiteList.whitelistedEmails))
  extends Filter {
  private val logger = LoggerFactory getLogger getClass

  def apply(next: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    environment match {
      case _ if ExampleEnviroConf.isItManagementOrAcceptanceTests(environment) || environment == "dev" => next(requestHeader)
      case _ =>
        (requestHeader.method, requestHeader.path) match {
          case ("GET", "/status") => next(requestHeader)
          case _ =>
            requestHeader.headers.get("ExampleClientCertificateSubject") match {
              case Some(requestSubjectHeader) =>
                val emailToVerify = Certificate.fromSubjectString(requestSubjectHeader).email
                if (whitelist.isAuthorised(emailToVerify))
                  next(requestHeader)
                else {
                  val whitelistError = s"Request from [$emailToVerify] not authorized."
                  logger warn whitelistError
                  Future.successful(Forbidden(whitelistError))
                }
              case None =>
                Future.successful(Unauthorized("Invalid request - certificate not provided."))
            }
        }
    }
  }
}
