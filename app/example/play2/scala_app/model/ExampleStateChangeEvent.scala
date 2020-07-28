package example.play2.scala_app.model

import play.api.libs.json.{JsValue, _}

case class ExampleStateChangeEvent(abstrObjId: String, username: String, activity: String, timestamp: Long) {
  def toJson: JsValue = Json.toJson(this)
  def toJsonString = Json.stringify(toJson)

  implicit val postUpdateEventWrites = new Writes[ExampleStateChangeEvent] {
    override def writes(statusUpdateEvent: ExampleStateChangeEvent): JsValue = Json.obj(
    "abstractObject" -> statusUpdateEvent.abstrObjId,
    "timestamp" -> statusUpdateEvent.timestamp,
    "activity" -> statusUpdateEvent.activity,
    "username" -> statusUpdateEvent.username
    )
  }
}
