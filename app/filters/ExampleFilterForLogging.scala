package filters

import scala.concurrent.Future

import play.api.mvc._
import play.api.Application

import org.slf4j.LoggerFactory

object ExampleFilterForLogging extends Filter {
  private val logger = LoggerFactory getLogger classOf[Application]

  def apply(nextFilter: (RequestHeader) => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {
    logger debug s"RequestHeader method [${requestHeader.method}] requestHeader uri [${requestHeader.uri}]."
    nextFilter(requestHeader)
  }
}
