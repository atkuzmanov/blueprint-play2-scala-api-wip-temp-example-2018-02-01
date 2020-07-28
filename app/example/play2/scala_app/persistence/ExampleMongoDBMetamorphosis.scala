package example.play2.scala_app.persistence

import example.play2.scala_app.Identifier
import com.mongodb.DBObject
import play.api.libs.json.Json._
import play.api.libs.json._

import scala.collection.Seq

object ExampleMongoDBMetamorphosis {

  val replaceMongoDB_IdWithId = {
    val addMongoDB_Id: Reads[JsObject] = __.json.update((__ \ 'id).json.copyFrom((__ \ '_id \ '$oid).json.pick))
    val pruneIdFromJson: Reads[JsObject] = (__ \ '_id).json.prune
    pruneIdFromJson compose addMongoDB_Id
  }

  def replaceIdWithMongoDB_Id(identifier: Identifier): Reads[JsObject] = {
    val pruneIdFromJson: Reads[JsObject] = (__ \ 'id).json.prune
    val addMongoIdentifier: Reads[JsObject] = __.json.update((__ \ '_id \ '$oid).json.put(JsString(identifier)))
    pruneIdFromJson compose addMongoIdentifier
  }

  val transmuteIdToMongoDB_Id: Reads[JsObject] = {
    val addId: Reads[JsObject] = __.json.update((__ \ '_id \ '$oid).json.copyFrom((__ \ 'id).json.pick))
    val removeId: Reads[JsObject] = (__ \ 'id).json.prune
    removeId compose addId
  }

  def dbObjectToPlay2FrameworkJsObject(jsonDBObject: DBObject): JsObject = {
    val jsonString: Identifier = com.mongodb.util.JSON.serialize(jsonDBObject)
    Json.parse(jsonString).as[JsObject]
  }

  def JsObjectPlay2FrameworkToDBObject(jsonObject: JsObject): DBObject = {
    val jsonString: String = Json.stringify(jsonObject)
    com.mongodb.util.JSON.parse(jsonString).asInstanceOf[DBObject]
  }

  def encapsulateAbstrObjWithMetadata(json: JsObject) = {
    obj(
      "id" -> (json \ "id"),
      "content" -> json)
  }

  def toUpdate(inputJsObject: JsObject): JsObject = {
    def traverseToUpdate(path: List[String], in: JsValue): Seq[(String, JsValue)] = in match {
      case JsObject(fields: Seq[(String, JsValue)]) =>
        fields flatMap {
          kv: (String, JsValue) => traverseToUpdate(kv._1 :: path, kv._2)
        }
      case value: JsValue => Seq(path.reverse.mkString(".") -> value)
    }
    JsObject(traverseToUpdate(List(), inputJsObject))
  }
}
