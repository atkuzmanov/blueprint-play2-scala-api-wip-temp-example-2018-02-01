package example.play2.scala_app.services

import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sns.model.PublishResult
import play.api.libs.json.{JsValue, Json}

case class ExampleAWSSNSClientDummy() extends AmazonSNSClient {
  val defaultSNSMsg: JsValue = Json.parse("{}")
  var latestSNSMsg: JsValue = defaultSNSMsg
  def reset(): Unit = {latestSNSMsg = defaultSNSMsg}

  override def publish(awsSNSArn: String, snsMsgContent: String): PublishResult = {
    val snsMsgAsJsValue: JsValue = Json.parse(snsMsgContent)
    latestSNSMsg = snsMsgAsJsValue
    new PublishResult().withMessageId((snsMsgAsJsValue \ "exampleContentField").as[String])
  }
}
