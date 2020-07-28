package example.play2.scala_app.integration

import example.play2.scala_app.persistence.ExampleAbstractObjectDAO
import example.play2.scala_app.ExampleEnviroConf.ExampleMongoDB
import example.play2.scala_app.persistence._

import scala.concurrent.ExecutionContextExecutor

trait ExampleDataAccess {
  def abstrObjsDAO: ExampleAbstractObjectDAO
}
class SimpleExampleDataAccess(implicit context: ExecutionContextExecutor) extends ExampleDataAccess {
  val abstrObjsDAO = new MongoExampleDataAccessObject(ExampleMongoDB.abstractObject) with ExampleDataAccessObjectDao
}


