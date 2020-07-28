package example.play2.scala_app.integration

import play.api.libs.json._


object ExampleJsonTransformers1 {

  val exampleMapping: Map[String, JsPath] = Map(
    "exampleId" -> __ \ "exampleId",
    "exampleText" -> __ \ "exampleText",
    "exampleURL" -> __ \ "exampleItem" \ "exampleUrls" \ "exampleUrl",
    "exampleTimestamp" -> __ \ "exampleTimestamp"
  )

  private def extractExampleItems(exampleInputJsObject: JsObject): JsResult[JsValue] = {
    exampleInputJsObject transform (__ \ "example" \ "collection" \ "items").json.pick
  }

  private def extractWithExampleMapping(exampleInputJsVal: JsValue, exampleMapping: Map[String, JsPath]): Map[String, JsValue] = {
    exampleMapping.mapValues(exampleInputJsVal transform _.json.pick).collect { case (k, v: JsSuccess[JsValue]) => (k, v.get) }
  }

  def exampleExtractFromJsObjUsingMapping(exampleInputJsObj: JsObject): Seq[JsObject] = {
    extractExampleItems(exampleInputJsObj) match {
      case JsSuccess(JsArray(Seq()), _) => Seq()
      case JsSuccess(exampleItems: JsArray, _) =>
        exampleItems.as[Seq[JsObject]] map { exampleItem => JsObject(replaceJsValueInMap(extractWithExampleMapping(exampleItem, exampleMapping)).toSeq) }
      case JsError(_) => Seq()
    }
  }

  private def replaceJsValueInMap(exampleInputMap: Map[String, JsValue]): Map[String, JsValue] = {
    exampleInputMap + ("exampleElement" -> exampleModifyValue(exampleInputMap("exampleElement")))
  }

  private def exampleModifyValue(exampleInputJsValue: JsValue): JsValue = {
    JsString(exampleInputJsValue.as[String] + "-" + System.currentTimeMillis())
  }

  private def exampleExtractFromJsValue1(exampleItemJsValue: JsValue): Option[JsString] = {
    (exampleItemJsValue \ "element1" \ "element2").asOpt[JsString]
  }

  private def exampleExtractFromJsValue2(exampleItemJsVal: JsValue): Seq[String] = {
    val extractedValues: Seq[JsValue] = exampleItemJsVal \ "element1" \ "element2" \\ "attribute1"
    if (extractedValues.isEmpty) Seq.empty[String] else extractedValues.map(_.as[String])
  }
}
