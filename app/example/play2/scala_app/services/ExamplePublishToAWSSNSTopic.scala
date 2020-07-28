package example.play2.scala_app.services

import com.amazonaws.services.sns.AmazonSNSClient
import example.play2.scala_app.ExampleEnviroConf.ExampleAWSSNSConfObj
import example.play2.scala_app.model.ExampleStateChangeEvent
import org.slf4j.{Logger, LoggerFactory}

import scala.util.{Failure, Success, Try}

class ExamplePublishToAWSSNSTopic(awsSNSClient: AmazonSNSClient) {
  private val logger: Logger = LoggerFactory getLogger getClass

  private def publishToSNSTopic(msgToSend: String): String = {
    Try(awsSNSClient.publish(ExampleAWSSNSConfObj.exampleSNSArn, msgToSend)) match {
      case Success(resultOfPublicationAttempt) => {
        logger.info(s"Successful publication to SNS topic of message with ID: [${resultOfPublicationAttempt.getMessageId}].")
        resultOfPublicationAttempt.getMessageId
      }
      case Failure(ex) => {
        logger.error(s"Error state on publication to SNS topic with exception: [${ex.getMessage}].", ex)
        throw ex
      }
    }
  }

  def sendAbstrObjChangeNotification(stateChangeEvent: ExampleStateChangeEvent): String = {
    publishToSNSTopic(stateChangeEvent.toJsonString)
  }
}

