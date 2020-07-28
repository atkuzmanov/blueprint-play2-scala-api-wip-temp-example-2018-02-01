package example.play2.scala_app.services

import java.util.UUID

import com.amazonaws.services.certificatemanager.model.InvalidStateException
import example.play2.scala_app.Identifier
import example.play2.scala_app.domain.{ExampleAbstractObject1, ExampleAbstractObjectTransmutators}
import example.play2.scala_app.model.{ExampleStateChange, ExampleStateChangeEvent}
import example.play2.scala_app.persistence.ExampleAbstractObjectDAO
import example.play2.scala_app.services.helpers.{ExampleAbstrObjContentTransformer, ExampleSystemDateTime}
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.concurrent._
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class ExampleAbstractObjService(exampleJsonTransformers2: ExampleAbstractObjectTransmutators,
                                abstrObjDAO: ExampleAbstractObjectDAO,
                                systemDateTime: ExampleSystemDateTime,
                                exampleUnimplementedAPIHitter: ExampleUnimplementedAPIHitter,
                                abstrObjChangeNotificationService: ExamplePublishToAWSSNSTopic
                               )(implicit val executor: ExecutionContext) {

  private lazy val log = LoggerFactory getLogger getClass

  def generateRandomUUID(): String = UUID.randomUUID().toString

  def saveAbstrObj(abstrObjToSave: JsObject): Future[Unit] = {
    abstrObjDAO.saveAbstrObj(abstrObjToSave)
  }

  def saveNewAbstrObj(exampleGuid: String, abstrObj: JsObject, username: String): Future[JsObject] = {
    val validatedExampleGuid = ExampleAbstrObjContentTransformer.isA32GuidStrictDashSeparatedGroups(exampleGuid) match {
      case true => Some(exampleGuid)
      case false => None
    }
    for {
      abstrObjToSave <- Future.successful(exampleJsonTransformers2.defaultConstructCreatedAbstrObj(systemDateTime.now, abstrObj, validatedExampleGuid, None, username, generateRandomUUID()))
      abstrObjId <- abstrObjDAO.createAbstrObj(abstrObjToSave)
    } yield abstrObjToSave ++ Json.obj("identifier" -> abstrObjId)
  }

  def getByAbstrObjIdentifier(identifier: Identifier): Future[Option[JsObject]] = {
    abstrObjDAO.readAbstrObj(identifier) map {
      _ flatMap { abstrObj => if (ExampleAbstractObject1.isDeleted(abstrObj)) None else Some(abstrObj) }
    }
  }

  def getExistingAbstrObj(abstrObjId: Identifier): Future[JsObject] = {
    abstrObjDAO.readAbstrObj(abstrObjId) flatMap {
      case Some(abstrObj) => {
        ExampleAbstractObject1.hasMandatoryElement(abstrObj) match {
          case false => {
            exampleJsonTransformers2.defaultNormaliseBody(abstrObj) match {
              case mainElement if (ExampleAbstrObjContentTransformer.isA32GuidStrictDashSeparatedGroups(mainElement.toString)) => {
                val abstrObjWithNewState = exampleJsonTransformers2.defaultConstructAddNewState(abstrObj, mainElement.toString)
//                Future.successful(abstrObjWithNewState)
                Future.successful(Json.obj("a" -> Json.parse(s"{}")))
              }
              case _ => Future.successful(abstrObj)
            }
          }
          case true => Future.successful(abstrObj)
        }
      }
      case None => Future.failed(new Exception("Err!"))
    }
  }


  def listAbstrObjsByGuid(guid: String, untilTimestamp: Option[Long], countLimit: Option[Int], state: Seq[String]): Future[JsObject] =
    abstrObjDAO.listAbstrObjs(untilTimestamp, countLimit, state, Some(guid))

  def delete(identifier: Identifier, username: String): Future[Boolean] = {
    val deletedObj: JsObject = exampleJsonTransformers2.defaultConstructDeletedAbstractObjUpdate(systemDateTime.now, username)

    abstrObjDAO.readAbstrObj(identifier) flatMap {
      case Some(abstrObj) if isAbstrObjDeleted(abstrObj) => Future.successful(true)

      case Some(abstrObj) if ExampleStateChange.getStateTransition(abstrObj, deletedObj).isDefined => {
        for {
          _ <- abstrObjDAO.updateAbstrObj(identifier, deletedObj)
          updatedAbstrObj <- abstrObjDAO.readAbstrObj(identifier)
          _ <- sendStateChangeNotification(abstrObj, deletedObj)
        } yield true
      }
      case _ => Future.successful(false)
    }
  }

  private def isAbstrObjDeleted(abstrObj: JsObject): Boolean = {
    val state: String = (abstrObj \ "metadata" \ "state").asOpt[String].getOrElse("")
    state == ExampleAbstractObject1.Deleted
  }

  def buildUpdatedPost(username: String, abstrObjId: Identifier, existingAbstrObj: JsObject, newAbstrPost: JsObject): Future[JsObject] = {
    Try(exampleJsonTransformers2.defaultConstructUpdatedAbstrObj(systemDateTime.now, username, abstrObjId, existingAbstrObj, normaliseAbstrObjElement1(newAbstrPost), generateRandomUUID())) match {
      case Success(updatedAbstrObj) => Future.successful(updatedAbstrObj)
      case Failure(e) => Future.failed(new InvalidStateException(s"Error when updating AbstrObj from existing abstrOjb: $existingAbstrObj and updated abstrOjb: $newAbstrPost"))
    }
  }

  def normaliseAbstrObjElement1(post: JsObject): JsObject = exampleJsonTransformers2.defaultNormaliseBody(post)

  def isItInAValidState(existingAbstrObj: JsObject, updatedAbstrObj: JsObject): Future[Boolean] = {
    ExampleStateChange.getStateTransition(existingAbstrObj, updatedAbstrObj) match {
      case Some(validState) => Future.successful(true)
      case None => Future.failed(new InvalidStateException("Invalid state transition."))
    }
  }

  def sendOutCreatedAbstrObj(abstrObj: JsObject): Future[Unit] = {
    if (ExampleAbstractObject1.isSentOut(abstrObj)) {
      sendOutWithRecovery(abstrObj)
    } else {
      Future.successful()
    }
  }

  def sendStateChangeNotification(existingAbstractObj: JsObject, updatedAbstrObj: JsObject): Future[String] = {
    val abstrObjId = (existingAbstractObj \ "identifier").as[String]
    val stateChange = ExampleStateChange.getStateTransition(existingAbstractObj, updatedAbstrObj)

    stateChange match {
      case Some(validStateTransition) =>
        val username = (updatedAbstrObj \ "metadata" \ "username").as[String]
        val sentOutTime = (updatedAbstrObj \ "metadata" \ "lastChangeTimestamp").as[Long]
        Future.successful(abstrObjChangeNotificationService.sendAbstrObjChangeNotification(ExampleStateChangeEvent(abstrObjId, username, validStateTransition, sentOutTime)))
      case None =>
        val errorMessage = "Could not send state change SNS notification!"
        log.error(errorMessage)
        Future.successful(errorMessage)
    }
  }

  private def sendOutWithRecovery(abstrObj: JsObject): Future[Unit] = {
    val identifier = (abstrObj \ "identifier").as[Identifier]
    abstrObjDAO.readAbstrObj(identifier) flatMap { retrievedAbstrObj =>
      exampleUnimplementedAPIHitter.publish(retrievedAbstrObj.get) flatMap {
        case Some(processingErrorResponse) =>
          val abstrObjWithError = retrievedAbstrObj.get ++ Json.obj("error" -> processingErrorResponse)
          log.error(s"Non-fatal error while processing AbstractObject [$identifier], processed with error information [$processingErrorResponse].")
          abstrObjDAO.saveAbstrObj(abstrObjWithError)
        case None => Future.successful()
      } recoverWith {
        case NonFatal(exeption) =>
          val errorJsonObj = JsObject(Seq("error" -> JsObject(Seq("class" -> JsString(this.getClass.toString), "error" -> JsString(exeption.getMessage), "fatal" -> JsBoolean(true)))))
          log.error(s"Exception when sending out abstrObj [$abstrObj] with identifier [$identifier] error information [$errorJsonObj].", exeption)
          val abstrObjWithTempState = exampleJsonTransformers2.defaultConstructAddNewState(retrievedAbstrObj.get, "temporary")
          val abstrObjWithTempStateAndRemovedMetadata = exampleJsonTransformers2.defaultDeleteMetadata(abstrObjWithTempState)
          val abstrObjWithErrors = abstrObjWithTempStateAndRemovedMetadata ++ errorJsonObj
          broadcastFailedFutureWithException(exeption) {
            abstrObjDAO.saveAbstrObj(abstrObjWithErrors)
          }
      }
    }
  }


  private def broadcastFailedFutureWithException[T](e: Throwable)(f: => Future[Any]) = {
    def handleResult: PartialFunction[Any, T] = {
      case _ => throw e
    }
    f map handleResult recover handleResult
  }
}

class AbstractObjectProcessingException(message: String) extends Exception(message)
