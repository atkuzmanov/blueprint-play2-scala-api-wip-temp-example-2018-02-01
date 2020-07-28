package appexample.play2.scala_app.endpoints.helpers

import java.net.URL

import org.apache.commons.lang3.StringUtils

import scala.util.Try

object ExampleHttpHelper {
  private def bitsWithoutDigits(text: String) = !StringUtils.containsAny(text, "0123456789")
  private def cleanUpDomainName(domainToCleanUp: String) = domainToCleanUp.split("\\.").headOption.getOrElse(domainToCleanUp)

  def hostExtractor(urlToExtractDomainFrom: String): Try[String] = {
    val possiblyValidUrl = Try {
      new URL(urlToExtractDomainFrom)
    }
    possiblyValidUrl.map(_.getHost)
  }

  def endpointFinder(endpointPath: String): String = {
    val endpointPathBits = endpointPath.stripPrefix("/").split("/")
    val requiredBits = endpointPathBits.filter(bitsWithoutDigits _)
    requiredBits.mkString(".")
  }

  def serviceIdentifier(url: String): String = cleanUpDomainName(hostExtractor(url).getOrElse(""))
}

