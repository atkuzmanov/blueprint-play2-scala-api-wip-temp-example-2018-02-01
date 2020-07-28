package example.play2.scala_app.services

import example.play2.scala_app.persistence.parsers.{ExampleHtmlBodyParser, ExampleHtmlBodyParserHelperException}
import play.api.libs.json.JsObject

import scala.concurrent.Future
import scala.util.{Failure, Try}

object ExampleAbstractObjectValidator {
  private def exampleHTMLPayloadElementValidator(payload: JsObject): Boolean = {
    val elementType = (payload \ "elementType").asOpt[String]
    elementType match {
      case Some("textElement") =>
        Try {
          ExampleHtmlBodyParser(payload.toString())
        } match {
          case Failure(_) => false
          case _ => true
        }
      case _ => true
    }
  }

  def exampleHTMLValidator(abstrObj: JsObject, errorMsg: String): Future[Unit] = {
    val payloadElems: Option[List[JsObject]] = (abstrObj \ "htmlBody").asOpt[List[JsObject]]
    payloadElems match {
      case None => Future.successful()
      case Some(elements) =>
        elements.exists(!exampleHTMLPayloadElementValidator(_)) match {
          case false => Future.successful()
          case true => Future.failed(new ExampleHtmlBodyParserHelperException(errorMsg))
        }
    }
  }
}


