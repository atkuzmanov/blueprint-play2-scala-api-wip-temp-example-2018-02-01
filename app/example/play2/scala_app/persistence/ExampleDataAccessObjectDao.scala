package example.play2.scala_app.persistence

import com.mongodb._
import example.play2.scala_app.ExampleEnviroConf
import example.play2.scala_app.ExampleEnviroConf.ExampleMongoDB._
import org.bson.types.ObjectId
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.JsObject
import example.play2.scala_app.persistence.ExampleMongoDBMetamorphosis._

import scala.concurrent._
import scala.util.{Failure, Success, Try}

trait ExampleDataAccessObjectDao {
  type DaoIdentifier = String

  // CRUD = C.R.U.D. = Create Read Update Delete

  def mdbCreate(json: JsObject): Future[Unit]

  def mdbRead(id: DaoIdentifier): Future[Option[JsObject]]

  def mdbUpdate(id: DaoIdentifier, json: JsObject): Future[Unit]

  def mdbDelete(id: DaoIdentifier): Future[Unit]

  def mdbDeleteAll(): Future[Unit]

  def mdbSave(json: JsObject): Future[Unit]
}

class MongoExampleDataAccessObject(mongoDBCollectionName: String)(implicit val context: ExecutionContextExecutor) extends ExampleDataAccessObjectDao {

  import play.api.libs.json.Json._

  private lazy val logger: Logger = LoggerFactory getLogger getClass

  protected[this] val mdbCollection = ExampleEnviroConf.ExampleMongoDB.mongodbDatabase.getCollection(mongoDBCollectionName)
//  protected[this] val mdbCollection = ExampleMongoDB.abstractObjectsMDBCollection
  protected[this] val allInsideCollection = obj()

  def compressIds(jsonObj: JsObject) = jsonObj.transform(ExampleMongoDBMetamorphosis.replaceMongoDB_IdWithId).get

  // CRUD = C.R.U.D. = Create Read Update Delete

  def mdbCreate(jsonObj: JsObject): Future[Unit] = {
    val dbObjToPersist: DBObject = ExampleMongoDBMetamorphosis.JsObjectPlay2FrameworkToDBObject(jsonObj)
    Future {
      throwOnErrorInNestedBlock {
        mdbCollection.save(dbObjToPersist, ExampleEnviroConf.ExampleMongoDB.mdbWriteConcern)
      }
    }
  }

  def mdbRead(daoId: DaoIdentifier): Future[Option[JsObject]] = {
    Try(new ObjectId(daoId)) match {
      case Failure(ex) => Future.successful(None)
      case Success(idObject) => Future {
        val searchId: BasicDBObject = new BasicDBObject("_id", idObject)
        val mdbObject: DBObject = mdbCollection.findOne(searchId)
        if (mdbObject == null) None else Some(compressIds(ExampleMongoDBMetamorphosis.dbObjectToPlay2FrameworkJsObject(mdbObject)))
      }
    }
  }

  def mdbUpdate(daoIdentifier: DaoIdentifier, jsObj: JsObject): Future[Unit] = {
    Try(new ObjectId(daoIdentifier)) match {
      case Failure(ex) => Future.failed(ex)
      case Success(idObject) => Future {
        val toUpdate = toUpdate(jsObj)
        val mdbObjToPersist: DBObject = ExampleMongoDBMetamorphosis.JsObjectPlay2FrameworkToDBObject(toUpdate)
        val dbObjModifier: BasicDBObject = new BasicDBObject("$set", mdbObjToPersist)
        val searchId = new BasicDBObject("_id", idObject)
        throwOnErrorInNestedBlock {
          mdbCollection.update(searchId, dbObjModifier, true, false, ExampleEnviroConf.ExampleMongoDB.mdbWriteConcern)
        }
      }
    }
  }

  def mdbUpdateInsert(mdbQuery: JsObject, jsonDocument: JsObject): Future[Unit] = {
    Future {
      throwOnErrorInNestedBlock {
        val mdbQuery = JsObjectPlay2FrameworkToDBObject(mdbQuery)
        val mdbDocument = JsObjectPlay2FrameworkToDBObject(obj("$set" -> jsonDocument))
        mdbCollection.update(mdbQuery, mdbDocument, true, false, mdbWriteConcern)
      }
    }
  }

  def mdbDelete(daoIdentifier: DaoIdentifier): Future[Unit] = {
    Try(new ObjectId(daoIdentifier)) match {
      case Failure(e) => Future.failed(e)
      case Success(identifierObject) => Future {
        val searchIdentifier: BasicDBObject = new BasicDBObject("_id", identifierObject)
        throwOnErrorInNestedBlock {
          mdbCollection.remove(searchIdentifier, mdbWriteConcern)
        }
      }
    }
  }

  def mdbDeleteAll(): Future[Unit] = {
    Future {
      val anyRecordMDBObj: BasicDBObject = new BasicDBObject()
      val writeResult: WriteResult = mdbCollection.remove(anyRecordMDBObj, mdbWriteConcern)
      writeResult.getLastError.throwOnError()
    }
  }

  def mdbSave(jsonObject: JsObject): Future[Unit] = {
    val jsonWithIdentifier: JsObject = jsonObject.transform(transmuteIdToMongoDB_Id).get
    val mdbObjToPersist: DBObject = JsObjectPlay2FrameworkToDBObject(jsonWithIdentifier)
    Future {
      throwOnErrorInNestedBlock {
        mdbCollection.save(mdbObjToPersist, mdbWriteConcern)
      }
    }
  }

  def removeUsingMDBQuery(jsonQuery: JsObject) = {
    Future {
      throwOnErrorInNestedBlock {
        val mdbQueryObject: DBObject = JsObjectPlay2FrameworkToDBObject(jsonQuery)
        mdbCollection.remove(mdbQueryObject, mdbWriteConcern)
      }
    }
  }

  private def throwOnErrorInNestedBlock(writeBlock: => WriteResult): Unit = {
    val writeResult = writeBlock
    writeResult.getLastError.throwOnError()
  }
}

