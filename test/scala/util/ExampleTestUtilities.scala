package util

import scala.concurrent.duration._
import scala.concurrent.{Await, Awaitable}

import play.api.libs.json.{JsObject, Reads}
import play.api.libs.json.Json._

import java.io.InputStream

trait ExampleTestUtilities {
  def complete[T](awaitableFuture: => Awaitable[T]): T = Await.result(awaitableFuture, 7.seconds)

  def fixtureAs[T](pathToFile: String)(implicit fjs: Reads[T]): T = {
    parse {
      val exampleInputStream: InputStream = getClass.getResourceAsStream(pathToFile)
      io.Source.fromInputStream(exampleInputStream).mkString
    }.as[T]
  }

  def fixture(pathToFile: String): Any = {
    val exampleInputStream: InputStream = getClass.getResourceAsStream(pathToFile)
    io.Source.fromInputStream(exampleInputStream).mkString
  }

  def exampleReadFixtureAsJson(pathToFile: String): JsObject = {
    parse {
      val exampleInputStream: InputStream = getClass.getResourceAsStream(pathToFile)
      io.Source.fromInputStream(exampleInputStream).mkString
    }.as[JsObject]
  }
}
