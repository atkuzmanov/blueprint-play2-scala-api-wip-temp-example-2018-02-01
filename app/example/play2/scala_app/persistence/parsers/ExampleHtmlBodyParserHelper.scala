package example.play2.scala_app.persistence.parsers

import play.api.libs.json.JsObject

import scala.util.parsing.combinator.RegexParsers

// BodyElement, BodyListItem, Link ... etc. are content objects or case classes which need to be implemented.
trait ExampleHtmlBodyParserHelper extends RegexParsers {
  override def skipWhitespace: Boolean = false

//  def defaultEntryPoint: Parser[List[BodyElement]]
  def defaultEntryPoint: Parser[List[JsObject]]

  type ExampleParserPreprocessor = (String) => String

  val examplePreprocessor: ExampleParserPreprocessor = identity

//  def apply(defaultInput: String): List[BodyElement] = {
  def apply(defaultInput: String): List[JsObject] = {
    parseAll(defaultEntryPoint, examplePreprocessor(defaultInput)) match {
      case Success(successResult, _) => successResult
      case failure: NoSuccess =>
        throw new ExampleHtmlBodyParserHelperException(s"Error while parsing body. " +
          s"Exception [${failure.msg}] for input [$defaultInput].")
    }
  }
}

class ExampleHtmlBodyParserHelperException(message: String) extends Exception(message)
