package example.play2.scala_app.persistence

import com.mongodb.DBCursor
import example.play2.scala_app.Identifier
import example.play2.scala_app.domain.ExampleAbstractObject1
import example.play2.scala_app.services.helpers.ExampleAbstrObjContentTransformer
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.JsObject
import play.api.libs.json.Json._

import scala.collection.JavaConversions._
import scala.concurrent.Future

trait ExampleAbstractObjectDAO {
  //CRUD = C.R.U.D. = Create Read Update Delete
  def createAbstrObj(jsonObj: JsObject): Future[Identifier]
  def readAbstrObj(identifier: Identifier): Future[Option[JsObject]]
  def updateAbstrObj(identifier: Identifier, json: JsObject): Future[Unit]
  def deleteAbstractObject(identifier: Identifier): Future[Unit]
  def deleteAllAbstractObjects(): Future[Unit]
  def saveAbstrObj(jsonObj: JsObject): Future[Unit]
  def listAbstrObjs(untilTime: Option[Long], countLimit: Option[Int], state: Seq[String], abstrObjId: Option[String]): Future[JsObject]
}

trait ExampleMongoDBAbstractObjectDAO extends ExampleAbstractObjectDAO { this: MongoExampleDataAccessObject =>
  private def newObjectId: Identifier = new ObjectId().toString
  private lazy val logger: Logger = LoggerFactory getLogger getClass

  def createAbstrObj(jsonObj: JsObject): Future[String] = {
    val identifier: Identifier = (jsonObj \ "identifier").asOpt[String] getOrElse newObjectId
    val jsonWithIdentifier: JsObject = jsonObj.transform(ExampleMongoDBMetamorphosis.replaceIdWithMongoDB_Id(identifier)).get
    mdbCreate(jsonWithIdentifier) map { _ => identifier}
  }

  def readAbstrObj(daoIdentifier: DaoIdentifier): Future[Option[JsObject]] = {
    mdbRead(daoIdentifier) map {
      case None => None
      case Some(someJson) => Some(ExampleAbstrObjContentTransformer.transmutateGuidsFromListOfUris(someJson))
    }
  }

  def updateAbstrObj(daoId: DaoIdentifier, jsonObj: JsObject): Future[Unit] = mdbUpdate(daoId, jsonObj)

  def deleteAbstractObject(identifier: DaoIdentifier): Future[Unit] = mdbDelete(identifier)

  def deleteAllAbstractObjects(): Future[Unit] = mdbDeleteAll()

  def saveAbstrObj(jsonObj: JsObject): Future[Unit] = mdbSave(jsonObj)

  // MongoDB index = listByTagIndex
  def listAbstrObjs(untilTimestamp: Option[Long], countLimit: Option[Int], state: Seq[String], abstractId: Option[String]): Future[JsObject] = {
    val mongoDBQuery: JsObject = List(untilTimestamp.map(untilLastUpdatedTimestamp), byState(state), abstractId.map(byAbstractId)).flatten.foldLeft(allInsideCollection)(_ ++ _)
    runListMongoDBQuery(mongoDBQuery, countLimit)
  }

  def runListMongoDBQuery(mongoDBQuery: JsObject, countLimit: Option[Int]): Future[JsObject] = {
    val mongoDBSortBy: JsObject = obj("metadata.sentOut" -> -1, "metadata.lastUpdated" -> -1)
    Future {
      val mongoDBResult: DBCursor = mdbCollection.
      find(ExampleMongoDBMetamorphosis.JsObjectPlay2FrameworkToDBObject(mongoDBQuery)).
      sort(ExampleMongoDBMetamorphosis.JsObjectPlay2FrameworkToDBObject(mongoDBSortBy)).
      limit(countLimit getOrElse Int.MaxValue)
      val mongoDBQueryResult: List[JsObject] = mongoDBResult.iterator.toList map ExampleMongoDBMetamorphosis.dbObjectToPlay2FrameworkJsObject
      obj("results" -> toJson(mongoDBQueryResult map compressIds map ExampleAbstrObjContentTransformer.transmutateGuidsFromListOfUris map surroundAbstrObjWithMetaData))
    }
  }

  private def byListOfIdentifiers(listOfAbstractIds: Seq[String]): Some[JsObject] = {
    val abstrObjIdsList: Seq[JsObject] = listOfAbstractIds map { abstrId => obj("$oid" -> abstrId) }
    Some(obj("_id" -> obj("$in" -> abstrObjIdsList)))
  }

  private def byAbstractId(abstractId: String): JsObject = {
    val fullUrl: Option[String] = ExampleAbstrObjContentTransformer.transmutateGuidToUri(abstractId)
    fullUrl match {
      case None => obj("metadata.urls" -> obj("$in" -> List(abstractId)))
      case Some(url) => obj("metadata.urls" -> obj("$in" -> List(abstractId,url)))
    }
  }

  private def byState(statesSequence: Seq[String]): Option[JsObject] = {
    statesSequence match {
      case Nil => Some( obj("metadata.state" -> obj("$in" -> ExampleAbstractObject1.metadataStateValuesList.filter(_ != "Deleted"))))
      case stateValues => Some( obj("metadata.state" -> obj("$in" -> stateValues)) )
    }
  }

  private def untilLastUpdatedTimestamp(updatedTimestamp: Long) = obj("metadata.lastUpdatedTimestamp" -> obj("$lt"-> updatedTimestamp))

  private def surroundAbstrObjWithMetaData(json: JsObject) = ExampleMongoDBMetamorphosis.encapsulateAbstrObjWithMetadata(json)
}


