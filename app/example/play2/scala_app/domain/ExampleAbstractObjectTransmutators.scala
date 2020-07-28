package example.play2.scala_app.domain

import app.example.play2.scala_app.ExampleEnviroConf
import org.joda.time.DateTime
import play.api.libs.json._
import app.example.play2.scala_app.domain.ExampleAbstractObject1._

class ExampleAbstractObjectTransmutators {

  private def defaultOptionalExtract[T](extractionPath: JsPath, extractionValue: JsValue)(implicit reads: Reads[T]): Option[T] = extractionPath.readNullable[T].reads(extractionValue).get

  private def defaultApplyTransmutations(abstrObj: JsObject)(defSequence: Seq[Reads[JsObject]]) = abstrObj.transform(defSequence.reduce(_ andThen _)).get

  private def defaultTransmutate(abstrObj: JsObject)(seq: Reads[JsObject]*): JsObject = abstrObj.transform(seq.reduce(_ andThen _)).get

  private def defaultGenericExtract[T](extractionPath: JsPath, extractionValue: JsValue)(implicit reads: Reads[T]): T = extractionPath.read[T].reads(extractionValue).get

  private val defaultAddCreatedByUser: (String) => Reads[JsObject] = (user: String) => __.json.update(defaultCreatedByUserPath.json.put(JsString(user)))

  private val defaultAddIdentifier: (String) => Reads[JsObject] = (identifier: String) => __.json.update(defaultIdentifierPath.json.put(JsString(identifier)))

  private val defaultAddState: (String) => Reads[JsObject] = (updatedState: String) => __.json.update(defaultStatePath.json.put(JsString(updatedState)))

  private val defaultAddCreatedMetadata: (DateTime, String) => Reads[JsObject] = {
    (now: DateTime, username: String) => defaultAddCreatedTimestamp(now.toDate.getTime) compose defaultAddCreatedByUser(username)
  }
  private val defaultAddCreatedTimestamp: (Long) => Reads[JsObject] = (timestamp: Long) => __.json.update(defaultCreatedTimestampPath.json.put(JsNumber(timestamp)))

  private val defaultAddUpdatedByUser: (String) => Reads[JsObject] = (user: String) => __.json.update(defaultLastUpdatedByUserPath.json.put(JsString(user)))

  private val defaultAddLastUpdatedTime: (Long) => Reads[JsObject] = (timestamp: Long) => __.json.update(defaultLastUpdatedTimestampPath.json.put(JsNumber(timestamp)))

  private val defaultAddUpdatedMetadata: (DateTime, String) => Reads[JsObject] = (currentDateTime: DateTime, user: String) => defaultAddLastUpdatedTime(currentDateTime.toDate.getTime) compose defaultAddUpdatedByUser(user)

  private val defaultRemoveError: Reads[JsObject] = defaultErrorPath.json.prune

  private val defaultAddLanguage: (String) => Reads[JsObject] = (lang: String) => __.json.update(defaultLingoPath.json.put(JsString(lang)))

  private val defaultRemoveMetadata: Reads[JsObject] = defaultMetadataPath.json.prune

  private val defaultAddMandatoryElem: (String) => Reads[JsObject] = (exampleId: String) => __.json.update(defaultMandatoryElementPath.json.put(JsString(exampleId)))

  private val defaultDeleteMetadataCreatedByUser: Reads[JsObject] = defaultCreatedByUserPath.json.prune

  private val dfaultDeleteMetadataCreatedTime: Reads[JsObject] = defaultCreatedTimestampPath.json.prune

  private def defaultChangeNBSPToSpace(value: String): String = value.replaceAll("&nbsp;", " ")

  private def defaultReplaceStrWithJsonTransm: (JsObject, (String) => String, JsPath) => Reads[JsObject] = {
    (jsonObj: JsObject, strTransmutator: String => String, jsPath: JsPath) => {
      val probableBody: Option[JsString] = jsPath.asSingleJson(jsonObj).asOpt[JsString]
      if (probableBody.isDefined) {
        val updatedBody: String = strTransmutator(probableBody.get.value)
        __.json.update(jsPath.json.put(JsString(updatedBody)))
      } else __.json.pickBranch
    }
  }

  private def defaultNormaliseBodyModel: (JsObject) => Reads[JsObject] = {
    (jsonObj: JsObject) => {
      val possibleBody: Option[List[JsValue]] = (jsonObj \ "bodyElement").asOpt[List[JsValue]]
      if (possibleBody.isDefined) {
        val updatedBody: List[JsValue] = defaultRegularizeBodyElements(possibleBody.get)
        __.json.update((__ \ 'bodyElement).json.put(JsArray(updatedBody)))
      } else __.json.pickBranch
    }
  }

  private val defaultRemoveEmptyBlocks: (JsObject) => Reads[JsObject] = {
    (jsonObj: JsObject) => {
      val probableBody: Option[List[JsValue]] = (jsonObj \ "bodyElement").asOpt[List[JsValue]]
      if (probableBody.isDefined) {
        val updatedBody: List[JsValue] = defaultSieveEmptyBlocks(probableBody.get)
        __.json.update((__ \ 'bodyElement).json.put(JsArray(updatedBody)))
      } else __.json.pickBranch
    }
  }

  def defaultNormaliseBody(json: JsObject): JsObject = {
    val nonBreakingSpaceTransmutations: Seq[Reads[JsObject]] = Seq.empty :+ defaultNormaliseBodyModel(json)
    defaultApplyTransmutations(json)(nonBreakingSpaceTransmutations)
  }

  def defaultJsonReplaceStr(jsonObj: JsObject, strTransmutator: String => String, jsPath: JsPath): JsValue = {
    val transmutator: Seq[Reads[JsObject]] = Seq.empty :+ defaultReplaceStrWithJsonTransm(jsonObj, strTransmutator, jsPath)
    defaultApplyTransmutations(jsonObj)(transmutator)
  }

  private val defaultCopyMetadata: (JsObject) => Reads[JsObject] = {
    (post: JsObject) => {
      defaultAddCreatedByUser(defaultGenericExtract[String](defaultCreatedByUserPath, post)) andThen
        defaultAddCreatedTimestamp(defaultGenericExtract[Long](defaultCreatedTimestampPath, post)) andThen
        defaultAddIdentifier(defaultGenericExtract[String](defaultIdentifierPath, post))
    }
  }

  private def defaultRegularizeBodyElements(list: List[JsValue]): List[JsValue] = {
    list map { defaultBlock =>
      val modelType = (defaultBlock \ "bodyElement1").as[String]
      modelType match {
        case _ => defaultBlock
        case "bodyText" => defaultJsonReplaceStr(defaultBlock.as[JsObject], defaultChangeNBSPToSpace, __ \ 'bodyTextModel)
      }
    }
  }

  private def defaultSieveEmptyBlocks(toBeSievedList: List[JsValue]): List[JsValue] = {
    toBeSievedList filter { defaultBlock =>
      (defaultBlock \ "defaultBodyElement").asOpt[String] match {
        case None => true
        case Some(str) => !str.isEmpty
      }
    }
  }

  private val defaultAddExampleMetadata: (String) => Reads[JsObject] = {
    (defaultIdentifier: String) => defaultAddMandatoryElem(defaultIdentifier)
  }

  private val defaultAddLanguageOrDefault: (JsObject) => Reads[JsObject] = {
    (abstrObj: JsObject) =>
      defaultAddLanguage(defaultOptionalExtract[String](defaultLingoPath, abstrObj).getOrElse(ExampleEnviroConf.defaultLanguage))
  }

  def defaultConstructUpdatedAbstrObj(currentDateTime: DateTime, defaultUser: String, defaultId: String, primaryAbstrObj: JsObject, changedAbstrObj: JsObject, defaultGuid: String): JsObject = {
    if (isSentOut(primaryAbstrObj) && isSentOut(changedAbstrObj)) {
      val defaultReadSequence: Seq[Reads[JsObject]] = Seq.empty :+
        defaultRemoveEmptyBlocks(changedAbstrObj) :+
        defaultCopyMetadata(primaryAbstrObj) :+
        defaultRemoveMetadata :+
        defaultAddUpdatedMetadata(currentDateTime, defaultUser) :+
        defaultAddLanguageOrDefault(changedAbstrObj) :+
        defaultAddIdentifier(defaultId)
      defaultTransmutate(changedAbstrObj)(
        defaultAmendReadSequenceForId(defaultAmendReadSequenceForId(defaultReadSequence, primaryAbstrObj), primaryAbstrObj): _*
      )
    } else if (isRemoved(changedAbstrObj)) {
      val readSeq = Seq.empty :+ defaultAddIdentifier(defaultId) :+
        defaultAddLanguageOrDefault(changedAbstrObj) :+
        defaultAddUpdatedMetadata(currentDateTime, defaultUser) :+
        defaultRemoveEmptyBlocks(changedAbstrObj)
      defaultTransmutate(primaryAbstrObj)(
        defaultAmendReadSequenceForId(defaultAmendReadSequenceForId(readSeq, primaryAbstrObj), primaryAbstrObj): _*
      )
    } else if (isSentOut(changedAbstrObj)) {
      val readSeq = Seq.empty :+ defaultAddIdentifier(defaultId) :+
        defaultRemoveMetadata :+
        defaultAddLanguageOrDefault(changedAbstrObj) :+
        defaultRemoveEmptyBlocks(changedAbstrObj) :+
        defaultAddUpdatedMetadata(currentDateTime, defaultUser)
      defaultTransmutate(changedAbstrObj)(
        defaultAmendReadSequenceForId(defaultAmendReadSequenceForId(readSeq, primaryAbstrObj), primaryAbstrObj): _*
      )
    } else {
      val readSeq = Seq.empty :+ defaultAddIdentifier(defaultId) :+
        defaultRemoveEmptyBlocks(changedAbstrObj) :+
        defaultAddUpdatedMetadata(currentDateTime, defaultUser) :+
        defaultAddLanguageOrDefault(changedAbstrObj) :+
        defaultRemoveMetadata
      defaultTransmutate(changedAbstrObj)(
        defaultAmendReadSequenceForId(defaultAmendReadSequenceForId(readSeq, primaryAbstrObj), primaryAbstrObj): _*
      )
    }
  }

  def defaultConstructCreatedAbstrObj(currentDateTime: DateTime, jsonObj: JsObject, defaultId: Option[String], optId: Option[String], defaultUser: String, defaultGuid: String): JsObject = {
    val mainTransmutations: Seq[Reads[JsObject]] = Seq.empty :+
      defaultAddUpdatedMetadata(currentDateTime, defaultUser) :+
      defaultRemoveMetadata :+
      defaultAddCreatedMetadata(currentDateTime, defaultUser) :+
      defaultAddLanguageOrDefault(jsonObj) :+
      defaultRemoveEmptyBlocks(jsonObj)

    val basicTransmutationsWithCurationId: Seq[Reads[JsObject]] =
      if (defaultId.isDefined)
        mainTransmutations :+ defaultAddExampleMetadata(defaultId.get)
      else
        mainTransmutations

    val totalTransmutations =
      if (isSentOut(jsonObj))
        basicTransmutationsWithCurationId :+ defaultAddCreatedMetadata(currentDateTime, defaultUser)
      else
        basicTransmutationsWithCurationId
    defaultApplyTransmutations(jsonObj)(totalTransmutations)
  }

  def defaultConstructAddNewState(currentState: JsObject, changedState: String) = {
    defaultTransmutate(currentState)(defaultAddState(changedState), defaultAddLanguageOrDefault(currentState))
  }

  def defaultDeleteMetadata(current: JsObject): JsObject = {
    defaultTransmutate(current)(
      defaultAddLanguageOrDefault(current),
      defaultDeleteMetadataCreatedByUser,
      dfaultDeleteMetadataCreatedTime
    )
  }

  private def defaultAmendReadSequenceForId(defaultReadSequence: Seq[Reads[JsObject]], primaryAbstrObj: JsObject): Seq[Reads[JsObject]] = {
    if (ExampleAbstractObject1.hasMandatoryElement(primaryAbstrObj)) {
      defaultReadSequence :+ defaultCopyMetadata(primaryAbstrObj)
    }
    else
      defaultReadSequence
  }

  def defaultConstructDeletedAbstractObjUpdate(currentDateTime: DateTime, defaultUser: String): JsObject = {
    defaultTransmutate(Json.obj())(
      defaultAddLastUpdatedTime(currentDateTime.toDate.getTime),
      defaultAddState("Deleted"),
      defaultAddUpdatedByUser(defaultUser)
    )
  }
}

