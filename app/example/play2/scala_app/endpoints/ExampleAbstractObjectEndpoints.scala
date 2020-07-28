package example.play2.scala_app.endpoints

import app.example.play2.scala_app
import app.example.play2.scala_app.ExampleEnviroConf.{ExampleAWSSNSConfObj, ExampleMongoDB}
import app.example.play2.scala_app.Identifier
import app.example.play2.scala_app.domain.ExampleAbstractObjectTransmutators
import app.example.play2.scala_app.endpoints.ExampleCustomHeaders
import app.example.play2.scala_app.model.{ExampleValidatorForJSONSchema, JsonSchemaException}
import app.example.play2.scala_app.monitoring.ExampleObserver
import app.example.play2.scala_app.persistence.{ExampleAbstractObjectDAO, MongoExampleDataAccessObject}
import app.example.play2.scala_app.persistence.parsers.ExampleHtmlBodyParserHelperException
import app.example.play2.scala_app.services.{ExampleAbstractObjectValidator, ExamplePublishToAWSSNSTopic, ExampleUnimplementedAPIHitter}
import app.example.play2.scala_app.services.helpers.ExampleDefaultSystemDateTime
import example.play2.scala_app.ExampleWSFacade
import example.play2.scala_app.services._
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._

//import example.play2.scala_app.ExampleEnviroConf.ExampleAWSSNSConfObj
//import example.play2.scala_app.domain.ExampleAbstractObjectTransmutators
//import example.play2.scala_app.model.ExampleValidatorForJSONSchema
//import example.play2.scala_app.persistence.ExampleAbstractObjectDAO
//import com.codahale.metrics.MetricRegistry
//import example.play2.scala_app.monitoring.ExampleObserver
//import example.play2.scala_app.persistence.parsers.ExampleHtmlBodyParserHelperException
//import example.play2.scala_app.services.helpers.ExampleDefaultSystemDateTime

import scala.concurrent.Future

trait ExampleAbstractObjectEndpoints {
  this: Controller =>

  private lazy val logger: Logger = LoggerFactory getLogger getClass

  protected val exampleAbstrObjService: ExampleAbstractObjService
  // keep, commented out on purpose
  // protected implicit val exampleMetricRegistry: com.codahale.metrics.MetricRegistry

  private def exampleNormalise(abstrObj: JsObject): JsObject = exampleAbstrObjService.normaliseAbstrObjElement1(abstrObj)

  private def exampleExtractNameOfUser(request: Request[_]) = request.headers.get(ExampleCustomHeaders.ExampleCustomHTTPHeader1)

  private lazy val ExampleUsernameRequiredError: Future[SimpleResult] =
    Future.successful(BadRequest(s"Username is required, missing header ['${ExampleCustomHeaders.ExampleCustomHTTPHeader1}']."))

  def exampleRetrieveByAbstrObjId(abstrObjId: Identifier): Action[AnyContent] = Action.async {
    exampleAbstrObjService.getByAbstrObjIdentifier(abstrObjId) map {
      case Some(jsonResult) => Ok(jsonResult)
      case None => NotFound
    }
  }

  def exampleListByAbstrObjId(abstrObjId: String, untilTimestamp: Option[Long], countLimit: Option[Int], state: Seq[String]) = {
    Action.async {
      exampleAbstrObjService.listAbstrObjsByGuid(abstrObjId, untilTimestamp, countLimit, state) map (Ok(_))
    }
  }

  def exampleCreateAbstrObj(): Action[JsValue] = Action.async(parse.json) { httpRequest =>
    ExampleObserver.exampleAsyncTiming("create-abstract-object") {
      exampleExtractNameOfUser(httpRequest) match {
        case Some(extractedUsername) => {
          val requestPayload = httpRequest.body.as[JsObject]
          for {
            _ <- ExampleValidatorForJSONSchema.validationOfFutureJSON("abstr-obj-update", requestPayload)
            newlyMintedGuid: String = exampleAbstrObjService.generateRandomUUID()
            post <- exampleAbstrObjService.saveNewAbstrObj(newlyMintedGuid, exampleNormalise(requestPayload), extractedUsername)
            _ <- exampleAbstrObjService.sendOutCreatedAbstrObj(post)
            response <- createResponseForSuccess(post)
          } yield response
        } recover {
          case ex: Exception => createResponseForFailure(ex, httpRequest)
        }
        case _ => ExampleUsernameRequiredError
      }
    }
  }

  // keep, commented out on purpose
  //  def updateAbstrObj(abstrObjId: Identifier): Action[JsValue] = {
  //    Action.async(parse.json) { restRequest =>
  //      ExampleObserver.exampleAsyncTiming("update-abstrObj") {
  //        exampleExtractNameOfUser(restRequest) match {
  //          case Some(extractedUsername) => {
  //            val requestPayload = restRequest.body.as[JsObject]
  //            updateAbstractObject(abstrObjId, extractedUsername, requestPayload)
  //          } recover {
  //            case e: Exception => {
  //              createResponseForFailure(e, restRequest)
  //            }
  //          }
  //          case _ => ExampleUsernameRequiredError
  //        }
  //      }
  //    }
  //  }

  def deleteAbstrObj(abstractObjectId: Identifier): Action[AnyContent] = {
    Action.async { asyncRequest =>
      delete(abstractObjectId, None)(asyncRequest)
    }
  }

  private def delete(abstrObjId: Identifier, exampleOptionalId: Option[String]) = Action.async { request =>
    exampleExtractNameOfUser(request) match {
      case Some(username) =>
        exampleAbstrObjService.delete(abstrObjId, username) map {
          case true => NoContent
          case _ => NotFound
        }
      case _ => ExampleUsernameRequiredError
    }
  }

  private def updateAbstractObject(abstrObjId: Identifier, nameOfUser: String, requestPayload: JsObject): Future[Unit] = {
    for {
      _ <- exampleRequestValidation(abstrObjId, requestPayload)
      existingPost <- exampleAbstrObjService.getExistingAbstrObj(abstrObjId)
      (publishedPost, response) <- processAbstrObjUpdate(abstrObjId, nameOfUser, existingPost, requestPayload, None)
      _ <- exampleAbstrObjService.buildUpdatedPost(nameOfUser, abstrObjId, existingPost, publishedPost)
    } yield response
  }


  private def processAbstrObjUpdate(abstrObjId: String, nameOfUser: String, abstrObjExisting: JsObject, payloadOfRequest: JsObject, exampleOptionalId: Option[String]): Future[(JsObject, SimpleResult)] = {
    for {
      _ <- exampleAbstrObjService.isItInAValidState(abstrObjExisting, payloadOfRequest)
      abstrObjUpdated <- exampleAbstrObjService.buildUpdatedPost(nameOfUser, abstrObjId, abstrObjExisting, payloadOfRequest)
      _ <- exampleAbstrObjService.saveAbstrObj(abstrObjUpdated)
      sentOutAbstrObj <- exampleAbstrObjService.buildUpdatedPost(nameOfUser, abstrObjId, abstrObjExisting, payloadOfRequest)
      _ <- exampleAbstrObjService.sendStateChangeNotification(abstrObjExisting, abstrObjUpdated)
      updateResponse <- buildSuccessfulUpdateResponse(sentOutAbstrObj)
    } yield (sentOutAbstrObj, updateResponse)
  }

  private def exampleRequestValidation(abstrObjId: String, payloadOfReq: JsObject): Future[Unit] = {
    for {
      _ <- ExampleValidatorForJSONSchema.validationOfFutureJSON("abstractObject-update", payloadOfReq)
      _ <- ExampleAbstractObjectValidator.exampleHTMLValidator(payloadOfReq, s"Exceptional state encountered when trying to parse HTML body with id [$abstrObjId].")
    } yield ()
  }

  private def questionExceptionForCauseOrMessage(exceptionToQuestion: Exception): String = {
    if (exceptionToQuestion.getCause == null) exceptionToQuestion.getMessage else exceptionToQuestion.getCause.getMessage
  }

  private def createResponseForSuccess(abstrObj: JsObject): Future[SimpleResult] = {
    //    Future.successful(Created.withHeaders(LOCATION -> routes.ExampleAbstractObjectEndpoints.exampleRetrieveByAbstrObjId((abstrObj \ "identifier").as[Identifier]).url))
    Future.successful(Created.withHeaders(LOCATION -> example.play2.scala_app.endpoints.ExampleAbstractObjectEndpoints.exampleRetrieveByAbstrObjId((abstrObj \ "identifier").as[Identifier]).toString()))
    //Future.successful(NoContent)
  }

  private def createResponseForFailure(exceptionToMatch: Exception, requestJsValue: Request[JsValue]): SimpleResult = {
    exceptionToMatch match {
      case ex: JsonSchemaException =>
        logger.info(s"${requestJsValue.uri} => ${ex.getMessage} => ${requestJsValue.body.as[JsObject]}")
        BadRequest("""{"error": "Request failed schema validation."}""")
      case ex: ExampleHtmlBodyParserHelperException =>
        BadRequest("Exceptional state encountered when trying to parse HTML body.")
      case ex: AbstractObjectProcessingException =>
        val detail = Json.obj("attention" -> "Exceptional state occured.") ++ Json.obj("exceptionMessage" -> ex.getMessage)
        InternalServerError(Json.prettyPrint(Json.obj("error" -> detail)))
      case ex =>
        InternalServerError(Json.obj("status" -> 500, "msg" -> questionExceptionForCauseOrMessage(ex)))
    }
  }

  private def buildSuccessfulUpdateResponse(publishedPost: JsObject) = {
    (publishedPost \ "failure").asOpt[JsObject] match {
      case Some(failureBlock) => Future.successful(Ok(failureBlock))
      case None => Future.successful(NoContent)
    }
  }
}

object ExampleAbstractObjectEndpoints extends Controller with ExampleAbstractObjectEndpoints {
  protected val exampleAbstrObjService = new ExampleAbstractObjService(
    exampleJsonTransformers2 = new ExampleAbstractObjectTransmutators,
    //    abstrObjDAO = new ExampleAbstractObjectDAO,
    abstrObjDAO = new ExampleAbstractObjectDAO {
      //CRUD = C.R.U.D. = Create Read Update Delete
      override def createAbstrObj(jsonObj: JsObject): Future[Identifier] = ???

      override def updateAbstrObj(identifier: Identifier, json: JsObject): Future[Unit] = ???

      override def deleteAllAbstractObjects(): Future[Unit] = ???

      override def readAbstrObj(identifier: Identifier): Future[Option[JsObject]] = ???

      override def deleteAbstractObject(identifier: Identifier): Future[Unit] = ???

      override def listAbstrObjs(untilTime: Option[Long], countLimit: Option[Int], state: Seq[String], abstrObjId: Option[String]): Future[JsObject] = ???

      override def saveAbstrObj(jsonObj: JsObject): Future[Unit] = ???
    },
    systemDateTime = ExampleDefaultSystemDateTime,
    exampleUnimplementedAPIHitter = new ExampleUnimplementedAPIHitter(new ExampleWSFacade),
    abstrObjChangeNotificationService = new ExamplePublishToAWSSNSTopic(ExampleAWSSNSConfObj()))

  //  protected implicit val exampleMetricRegistry: MetricRegistry = com.codahale.metrics.MetricRegistry

  protected val exampleMongoDBDAO = new MongoExampleDataAccessObject(ExampleMongoDB.abstractObject)
}