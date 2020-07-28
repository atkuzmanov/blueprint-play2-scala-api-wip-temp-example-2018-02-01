package example.play2.scala_app

import java.io.{File, FileInputStream}
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

import com.ning.http.client.AsyncHttpClientConfig.Builder
import com.ning.http.client.{AsyncHttpClientConfig, ProxyServer}
import example.play2.scala_app.monitoring.{ExampleLocalCodeStatsD, ExampleObserver}
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import play.api.libs.ws.ning.NingWSClient

import scala.concurrent.Future

trait ExampleHTTPFacadeTrait {
  //CRUD = C.R.U.D. = Create Read Update Delete
  def post(exampleHTTPFacadeReq: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse]    //Create
  def get(exampleHTTPFacadeReq: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse]     //Read
  def put(exampleHTTPFacadeReq: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse]     //Update
  def delete(exampleHTTPFacadeReq: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse]  //Delete
}

case class ExampleHTTPFacadeRequest(uri: String,
                                    parameters: Seq[(String, String)] = Seq(),
                                    headers: Seq[(String, String)] = Seq(),
                                    payload: Option[Any] = None) {

  def withHeader(key: String, value: String): ExampleHTTPFacadeRequest = {
    ExampleHTTPFacadeRequest(uri,parameters,headers :+ (key,value), payload)
  }

  def withParameter(key: String, value: String): ExampleHTTPFacadeRequest = {
    ExampleHTTPFacadeRequest(uri,parameters :+ (key,value) , headers, payload)
  }

  def withPayloadAsString(value: Option[String]): ExampleHTTPFacadeRequest = {
    ExampleHTTPFacadeRequest(uri, parameters, headers, value)
  }

  def withPayloadAsJson(value: Option[JsValue]): ExampleHTTPFacadeRequest = {
    ExampleHTTPFacadeRequest(uri, parameters, headers, value)
  }
}


case class ExampleHTTPFacadeResponse(statusCode: Int, headers: Map[String, Seq[String]], val response: WSResponse) {
  def body: String = {
    response.body
  }

  def header(key: String): Option[String] = {
    if(headers.contains(key))
      Some(headers.get(key).mkString(","))
    else
      None
  }

  def json: JsValue = {
    response.json
  }
}


class ExampleWSFacade extends ExampleHTTPFacadeTrait {
  private lazy val logger: Logger = LoggerFactory getLogger getClass

  private def pruneContent(contentString: String): String = {
    val prunedContent: String = """\n""".r.replaceAllIn(contentString.take(300), " ")
    """\s{2,}""".r.replaceAllIn(prunedContent, " ")
  }

  private def returnResponse(reqMethod: String, exampleReq: ExampleHTTPFacadeRequest, wsResp: WSResponse, startTime: Long) : ExampleHTTPFacadeResponse = {
    val responseTimeTaken: Long = System.currentTimeMillis() - startTime
    val reqParameters: String = exampleReq.parameters.mkString("&")
    val reqHeaders: String = exampleReq.headers.mkString(",")
    val errorResponseBody: String = if (!wsResp.body.isEmpty && (wsResp.status / 100) == 5) "errorResponseBody " + pruneContent(wsResp.body) else ""
    logger info s"$reqMethod request to [${exampleReq.uri}] with headers [$reqHeaders] and parameters [$reqParameters] " +
      s"returned status code [${wsResp.status}] for a [${wsResp.body.size}B] payload in [${responseTimeTaken}ms] ${errorResponseBody}."
    increaseStatistics(reqMethod, exampleReq, wsResp.status)
    ExampleHTTPFacadeResponse(wsResp.status, wsResp.allHeaders, wsResp)
  }

  //CRUD = C.R.U.D. = Create Read Update Delete

  //CRUD = C.R.U.D. - Create
  def post(exampleRequest: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse] = {
    val requestStart: Long = System.currentTimeMillis()
    ExampleObserver.exampleAsyncTiming(s"exampleRequest.${exampleRequest.uri}.post") {
      ExampleWSClient.ningWSClient.url(exampleRequest.uri)
        .withHeaders(exampleRequest.headers: _*)
        .withQueryString(exampleRequest.parameters: _*)
        .post(exampleRequest.payload match {
          case Some(reqPayload) => reqPayload.toString
          case _ => ""
        }) map (exampleResponse => returnResponse("POST", exampleRequest, exampleResponse, requestStart))
    }
  }

  //CRUD = C.R.U.D. - Read
  def get(exampleRequest: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse] = {
    val requestStartTime: Long = System.currentTimeMillis()
    ExampleObserver.exampleAsyncTiming(s"exampleRequest.${exampleRequest.uri}.get") {
      ExampleWSClient.ningWSClient.url(exampleRequest.uri)
        .withHeaders(exampleRequest.headers: _*)
        .withQueryString(exampleRequest.parameters: _*)
        .get() map (exampleResponse => returnResponse("GET", exampleRequest, exampleResponse, requestStartTime))
    }
  }

  //CRUD = C.R.U.D. - Update
  def put(exampleReq: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse] = {
    val requestStartTimestamp = System.currentTimeMillis()
    ExampleObserver.exampleAsyncTiming(s"exampleRequest.${exampleReq.uri}.put") {
      ExampleWSClient.ningWSClient.url(exampleReq.uri)
        .withHeaders(exampleReq.headers: _*)
        .withQueryString(exampleReq.parameters: _*)
        .put(exampleReq.payload match {
          case Some(reqPayload) => reqPayload.toString
          case _ => ""
        }) map (exampleResponse => returnResponse("PUT", exampleReq, exampleResponse, requestStartTimestamp))
    }
  }

  //CRUD = C.R.U.D. - Delete
  def delete(exampleRequest: ExampleHTTPFacadeRequest): Future[ExampleHTTPFacadeResponse] = {
    val requestStartTime = System.currentTimeMillis()
    ExampleObserver.exampleAsyncTiming(s"exampleRequest.${exampleRequest.uri}.delete") {
      ExampleWSClient.ningWSClient.url(exampleRequest.uri)
        .withHeaders(exampleRequest.headers: _*)
        .withQueryString(exampleRequest.parameters: _*)
        .delete() map (exampleResponse => returnResponse("DELETE", exampleRequest, exampleResponse, requestStartTime))
    }
  }

  private def increaseStatistics(method: String, exampleRequest: ExampleHTTPFacadeRequest, statusCode: Int) = {
    val key = s"exampleRequest.${exampleRequest.uri}.${method.toLowerCase}.$statusCode"
    logger.debug(s"increaseStats: $key")
    // increment stats
  }
}

private object ExampleWSClient {
  private lazy val logger: Logger = LoggerFactory getLogger getClass

  private val defaultTimeOutMillis: Int = 35000

  private val defaultProxyServer: ProxyServer = new ProxyServer(ProxyServer.Protocol.HTTP, ExampleEnviroConf.defaultProxyHost, 80)

  private def buildSSLContext(conf: ExampleEnviroConf.ExampleSSLConfiguration): SSLContext = {
    logger.debug(s"Building SSLContext with keysStore [${conf.keyStoreLocation}] and trustStore [${conf.trustStoreLocation}].")
    val keyStoreInstance: KeyStore = KeyStore.getInstance(conf.keyStoreType)
    val keyStoreFileFromInputStream: FileInputStream = new FileInputStream(new File(conf.keyStoreLocation))
    keyStoreInstance.load(keyStoreFileFromInputStream, conf.keyStorePassword.toCharArray)

    val trustStoreInstance: KeyStore = KeyStore.getInstance(conf.trustStoreType)
    val trustStoreFileFromInputStream: FileInputStream = new FileInputStream(new File(conf.trustStoreLocation))
    trustStoreInstance.load(trustStoreFileFromInputStream, conf.trustStorePassword.toCharArray)

    val defaultKeyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    defaultKeyManagerFactory.init(keyStoreInstance, conf.keyStorePassword.toCharArray)

    val defaultTrustManagerFactory: TrustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    defaultTrustManagerFactory.init(trustStoreInstance)

    val defaultSSLContext: SSLContext = SSLContext.getInstance("TLS")
    defaultSSLContext.init(defaultKeyManagerFactory.getKeyManagers, defaultTrustManagerFactory.getTrustManagers, new SecureRandom)
    defaultSSLContext
  }

  private def defaultBuilder: Builder = {
    val defaultBuilder: Builder = new AsyncHttpClientConfig.Builder
    defaultBuilder.setConnectionTimeoutInMs(defaultTimeOutMillis)
    defaultBuilder.setRequestTimeoutInMs(defaultTimeOutMillis)
    if (ExampleEnviroConf.exampleSSLConfiguration.isDefined) {
      defaultBuilder.setSSLContext(buildSSLContext(ExampleEnviroConf.exampleSSLConfiguration.get))
    }
    if (ExampleEnviroConf.defaultUseHttpProxy) {
      logger.debug(s"Http proxy set to: [${ExampleEnviroConf.defaultProxyHost}].")
      defaultBuilder.setProxyServer(defaultProxyServer)
    }
    defaultBuilder
  }

  val ningWSClient: NingWSClient = new play.api.libs.ws.ning.NingWSClient(defaultBuilder.build())
}
