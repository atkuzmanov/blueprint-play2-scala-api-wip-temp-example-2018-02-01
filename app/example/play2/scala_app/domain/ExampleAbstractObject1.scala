package example.play2.scala_app.domain

import play.api.libs.json.{JsObject, _}

object ExampleAbstractObject1 {
  val defaultIdentifierPath = __ \ 'default_identifier
  val defaultMetadataPath = __ \ 'default_metadata
  val defaultCreatedTimestampPath = defaultMetadataPath \ 'default_createdTimestamp
  val defaultCreatedByUserPath = defaultMetadataPath \ 'default_createdByUser
  val defaultStatePath = defaultMetadataPath \ 'default_state
  val defaultLastUpdatedTimestampPath = defaultMetadataPath \ 'default_lastUpdatedTimestamp
  val defaultLastUpdatedByUserPath = defaultMetadataPath \ 'default_updatedByUser
  val defaultMandatoryElementPath = defaultMetadataPath \ 'default_mandatoryElement
  val defaultLingoPath = defaultMetadataPath \ 'default_language
  val defaultErrorPath = __ \ 'default_error

  private def isState(json: JsObject, stateParam: String): Boolean = {
    defaultStatePath(json) match {
      case JsString(state) :: Nil => state == stateParam
      case _ => false
    }
  }

  def hasMandatoryElement(json: JsObject): Boolean = {
    defaultMandatoryElementPath(json) match {
      case JsString(mandatoryElement) :: Nil => true
      case _ => false
    }
  }

  def getStateOf(json: JsObject): Option[String] = {
    defaultStatePath(json) match {
      case JsString(state) :: Nil => Some(state)
      case _ => None
    }
  }

  val SentOut = "sentout"
  val Deleted = "deleted"
  val Removed = "removed" // Withdrawn but not deleted.
  val Drafted = "drafted"
  val Available = "available" // Ready and available, but not sent out.

  def isDrafted(json: JsObject) = isState(json, Drafted)
  def isAvailable(json: JsObject) = isState(json, Available)
  def isSentOut(json: JsObject) = isState(json, SentOut)
  def isRemoved(json: JsObject) = isState(json, Removed)
  def isDeleted(json: JsObject) = isState(json, Deleted)
  def isDraftedOrAvailable(json: JsObject) = isDrafted(json) || isAvailable(json)

  val metadataStateValuesList = List(SentOut, Deleted, Removed, Drafted, Available)
}

object AbstrObjActions extends Enumeration {
  // CRUD = C.R.U.D. = Create Read Update Delete
  val CREATE = Value("create")
  val READ = Value("read")
  val UPDATE = Value("update")
  val DELETE = Value("delete")
  val REMOVE = Value("remove")
}
