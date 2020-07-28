package example.play2.scala_app.model

import java.util

import com.fasterxml.jackson.databind.JsonNode
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.{LogLevel, ProcessingMessage, ProcessingReport}
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.JsObject

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.concurrent.Future

object ExampleValidatorForJSONSchema {
  private val defaultSchemasMap: mutable.HashMap[String, JsonSchema] = scala.collection.mutable.HashMap.empty[String, JsonSchema]

  private lazy val logger: Logger = LoggerFactory getLogger getClass

  def defaultJsonValidation(defaultSchema: String, jsonObj: JsObject): Option[String] = {
    val defaultValidator: JsonSchema = loadJsonSchema(defaultSchema)
    val jsonContentsAsNode: JsonNode = JsonLoader.fromString(jsonObj.toString())
    val processingReport: ProcessingReport = defaultValidator.validate(jsonContentsAsNode)
    val processingMessages: util.Iterator[ProcessingMessage] = processingReport.iterator
    val processingMessagesErred: Iterator[ProcessingMessage] = processingMessages.filter(
    message => message.getLogLevel == LogLevel.ERROR || message.getLogLevel == LogLevel.FATAL
    )
    if (processingMessagesErred.isEmpty) {
      None
    } else {
      Some(s"Request failed schema validation: ${processingMessagesErred.toArray.deep}"
        .replaceAll("\\s+"," ")
        .replaceAll("\"","'"))
    }
  }

  def validationOfFutureJSON(validationSchema: String, jsonJsObj: JsObject): Future[Unit] = {
    defaultJsonValidation(validationSchema, jsonJsObj) match {
      case None => Future.successful()
      case Some(errorMsg) => Future.failed(new JsonSchemaException(errorMsg))
    }
  }

  private def loadJsonSchema(schemaName: String): JsonSchema = {
    defaultSchemasMap getOrElse (schemaName, {
      val factory: JsonSchemaFactory = JsonSchemaFactory.byDefault()
      val exampleSchema: JsonSchema = factory.getJsonSchema(JsonLoader.fromResource(s"/schemas/$schemaName.schema.json"))
      logger.info(s"The $schemaName.schema.json has been successfully loaded.")
      defaultSchemasMap.put(schemaName, exampleSchema)
      exampleSchema
    })
  }
}

class JsonSchemaException(e: String) extends Exception(e)
