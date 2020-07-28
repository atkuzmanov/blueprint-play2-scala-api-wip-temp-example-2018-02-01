package example.play2.scala_app.model

import example.play2.scala_app.domain.ExampleAbstractObject1
import play.api.libs.json.JsObject

object ExampleStateChange {
  val SENDOUT = "sendOut" // Create - C.R.U.D. - CRUD
  val READ = "read"       // Read - C.R.U.D. - CRUD
  val UPDATE = "update"   // Update - C.R.U.D. - CRUD
  val DELETE = "delete"   // Delete - C.R.U.D. - CRUD
  val REMOVE = "remove"
  val UNDEFINED = "undefined"

  def getStateTransition(currentObject: JsObject, updatedObject: JsObject): Option[String] = {
    val existingState = ExampleAbstractObject1.getStateOf(currentObject).get
    val updatedState = ExampleAbstractObject1.getStateOf(updatedObject).get

    (existingState, updatedState) match {
      case (ExampleAbstractObject1.SentOut, ExampleAbstractObject1.SentOut) => Some(UPDATE)
      case (ExampleAbstractObject1.SentOut, ExampleAbstractObject1.Removed) => Some(REMOVE)
      case (ExampleAbstractObject1.Drafted, ExampleAbstractObject1.SentOut) => Some(SENDOUT)
      case (ExampleAbstractObject1.Drafted, ExampleAbstractObject1.Drafted) => Some(UPDATE)
      case (ExampleAbstractObject1.Drafted, ExampleAbstractObject1.Available) => Some(UPDATE)
      case (ExampleAbstractObject1.Drafted, ExampleAbstractObject1.Deleted) => Some(DELETE)
      case (ExampleAbstractObject1.Available, ExampleAbstractObject1.SentOut) => Some(SENDOUT)
      case (ExampleAbstractObject1.Available, ExampleAbstractObject1.Drafted) => Some(UPDATE)
      case (ExampleAbstractObject1.Available, ExampleAbstractObject1.Available) => Some(UPDATE)
      case (ExampleAbstractObject1.Available, ExampleAbstractObject1.Deleted) => Some(DELETE)
      case _ => None
    }
  }
}
