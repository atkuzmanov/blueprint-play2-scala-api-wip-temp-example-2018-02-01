package example.play2.scala_app.services.helpers

import play.api.libs.json._

import scala.util.matching.Regex

object ExampleAbstrObjContentTransformer {
  val exampleUriPath1Regex: Regex = s"http://www.example.com/examplePath1/(.*?)#id".r
  val exampleUriPath2Regex: Regex = s"http://www.example.com/examplePath2/(.*?)".r

  def isA32GuidStrictDashSeparatedGroups(guidString: String) = {
    val regexToParseA32GuidStrictDashSeparatedGroups: Regex = """(^[0-9a-fA-F]{8}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{4}\-[0-9a-fA-F]{12}?)""".r
    regexToParseA32GuidStrictDashSeparatedGroups.findFirstIn(guidString).isDefined
  }

  def isA32GuidNoStrictGroupDashSeparation(guidString: String) = {
    val regexToParseA32GuidNoStrictGroupDashSeparation: Regex = """(^[0-9A-F]{32}?)""".r
    regexToParseA32GuidNoStrictGroupDashSeparation.findFirstIn(guidString).isDefined
  }

  def isA24GuidNoStrictGroupDashSeparation(guidString: String) = {
    val regexToParseA24GuidNoStrictGroupDashSeparation: Regex = """(^[0-9A-F]{24}?)""".r
    regexToParseA24GuidNoStrictGroupDashSeparation.findFirstIn(guidString).isDefined
  }

  def transmutateGuidsFromListOfUris(inputJson: JsObject): JsObject = {
    val urisList: Option[List[String]] = (inputJson \ "guidsList").asOpt[List[String]]
    val extractedGuids: Option[List[String]] = {
      urisList match {
        case Some(uris) => Some(
          uris map {
            case exampleUriPath1Regex(uriGuid) => uriGuid
            case exampleUriPath2Regex(uriGuid) => uriGuid
            case uriGuid => uriGuid
          }
        )
        case None => None
      }
    }
    extractedGuids match {
      case Some(guid) => addGuids(removeUris(inputJson), guid) //Replace uris with extracted guids.
      case None => inputJson
    }
  }

  private def addGuids(jsonObjToManipulate: JsObject, guidsToAdd: List[String]): JsObject = {
    jsonObjToManipulate.transform(__.json.update((__ \ 'guidsList).json.put(JsArray(guidsToAdd map JsString)))).get
  }

  private def removeUris(jsonObjToManipulate: JsObject): JsObject = {
    jsonObjToManipulate.transform((__ \ 'guidsList).json.prune).get
  }

  def transmutateGuidToUri(guidToConvert: String): Option[String] = {
    if (isA32GuidStrictDashSeparatedGroups(guidToConvert))
      Some(s"http://www.example.com/examplePath1/$guidToConvert#id")
    else if (isA24GuidNoStrictGroupDashSeparation(guidToConvert) || isA32GuidNoStrictGroupDashSeparation(guidToConvert))
      Some(s"http://www.example.com/examplePath2/$guidToConvert")
    else
      None
  }
}
