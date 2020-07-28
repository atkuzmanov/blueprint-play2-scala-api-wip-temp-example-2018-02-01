package example.play2.scala_app.dummies

import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model._

import scala.collection.JavaConversions._
import scala.collection.mutable

class ExampleScalaSQSClientDummy extends AmazonSQSClient {
  var messagesOnSQS = new mutable.MutableList[ReceiveMessageResult]

  def getFirstMessage(): ReceiveMessageResult = {
    if (messagesOnSQS.isEmpty)
      new ReceiveMessageResult
    else
      messagesOnSQS.head
  }

  def deleteAllMessages(): Unit = messagesOnSQS = new mutable.MutableList[ReceiveMessageResult]

  override def sendMessage(exampleSQSUrl: String, exampleMessageBody: String): SendMessageResult = {
    val message = new Message().withBody(exampleMessageBody)
    val receiveMessageResult = new ReceiveMessageResult().withMessages(Seq(message))
    messagesOnSQS = messagesOnSQS :+ receiveMessageResult
    new SendMessageResult
  }
}
