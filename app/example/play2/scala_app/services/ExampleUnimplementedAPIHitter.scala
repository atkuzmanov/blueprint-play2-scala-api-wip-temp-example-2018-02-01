package example.play2.scala_app.services

import example.play2.scala_app.{ExampleHTTPFacadeResponse, ExampleWSFacade}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, XML}

class ExampleUnimplementedAPIHitter(exampleWSFacade: ExampleWSFacade)(implicit val executor: ExecutionContext) {

  private lazy val log = LoggerFactory getLogger getClass

  def publish(abstrObj: JsObject): Future[Option[JsObject]] = {
//    val payload = toPayload(abstrObj)
//      log.info(s"Writing post [$abstrObj] into content store as payload [$payload].")
//      exampleWSFacade.post(payload) flatMap { response =>
//        response.statusCode match {
//          case 200 =>
//            handle200Response(response)
//          case 204 =>
//            log.info("Post published successfully.")
//            Future.successful(None)
//          case _ =>
//            log.error(s"Unreckognised response.")
//            Future.failed(new Exception(response.body))
//        }
//      }
    ???
  }

  private def handle200Response(response: ExampleHTTPFacadeResponse): Future[Some[JsObject]] = {
    Try(response.json) match {
      case Success(json) =>
        (json \ "failure").asOpt[JsObject] match {
          case Some(failureBlock) => (failureBlock \ "fatal").as[Boolean] match {
            case true => Future.failed(new Exception(Json.prettyPrint(failureBlock)))
            case false =>
              log.info(s"Non-fatal error encountered during publishing. Returning failure block for reference: ${Json.prettyPrint(failureBlock)}")
              Future.successful(Some(failureBlock))
          }
          case None => Future.failed(new Exception(Json.prettyPrint(json)))
        }
      case Failure(nonJson) => Future.failed(new Exception(s"Error while parsing JSON."))
    }
  }

  private def tagsOf(post: JsObject) = (post \ "tags").asOpt[List[String]].getOrElse(List.empty)

  private def toPayload(post: JsObject)(content: String): Elem = {
    val tags = tagsOf(post)
    val payload =
      <payload>
        <document>
          { XML loadString content }
        </document>
        <tags>{ for (tag <- tags) yield <tag type="about">{tag}</tag> }</tags>
      </payload>

    payload.asInstanceOf[Elem]
  }
}

