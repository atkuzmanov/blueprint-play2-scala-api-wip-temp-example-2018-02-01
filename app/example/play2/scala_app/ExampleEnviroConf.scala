package example.play2.scala_app

import java.nio.file.{Files, Paths}

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.InstanceProfileCredentialsProvider
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.sns.AmazonSNSClient
import com.amazonaws.services.sqs.AmazonSQSClient
import com.mongodb._
import dispatch.{Http, url}
import example.play2.scala_app.dummies.ExampleScalaSQSClientDummy
import example.play2.scala_app.persistence.ExampleMongoDBMetamorphosis
import example.play2.scala_app.services.ExampleAWSSNSClientDummy
import org.apache.commons.lang3.StringUtils
import play.api.libs.json.Json._
import play.api.libs.json._

//import play.mvc.Http.Response
import sun.misc.BASE64Encoder

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.util.Try

object ExampleEnviroConf {

  private val deployedConfPath: String = "/etc/example/path/example-conf.json"
  private val isDeployedConfReadable: Boolean = Files.isReadable(Paths.get(deployedConfPath))
  //  private lazy val deployedConfAsJsValue: JsValue = Try(Json.parse(io.Source.fromFile(deployedConfPath).mkString)).getOrElse(Json.parse("{}"))
  private lazy val deployedConfAsJsValue: JsValue = {
    //  Try(Json.parse(io.Source.fromFile(deployedConfPath).mkString)).getOrElse(Json.parse("{}"))
    Json.parse("{}")
  }
  println(s">>> Configuration file [$deployedConfPath] exists and is readable [$isDeployedConfReadable].")

  private val defaultSysExecServerEnv: Option[String] = sys.env.get("DEFAULT_SYSTEM_EXECUTION_SERVER_ENV")
  private val defaultSysExecTestLevel: Option[String] = sys.env.get("DEFAULT_SYSTEM_EXECUTION_TEST_LEVEL")
  println(s">>> DEFAULT_SYSTEM_EXECUTION_SERVER_ENV is [${defaultSysExecServerEnv getOrElse "not set"}].")
  println(s">>> TEST_LEVEL is [${defaultSysExecTestLevel getOrElse "not set"}].")

  val defaultProxyHost = "example.default.proxy.com"
  val defaultUseHttpProxy = false
  val defaultLanguage: String = "en-gb"
  val defaultStubbedServerUrl: String = "http://localhost:9096"
  println(s">>> Default language is [$defaultLanguage].")

  lazy val mongodbListOfIPs: Option[String] = (deployedConfAsJsValue \ "conf" \ "list_of_mongodb_ips").asOpt[String]

  // Environment - dev-local, int-integration, test-testing, stage-staging, live-production, mgmt-management-jenkins...
  def exampleExecutionEnvironment: String = {
    if (isDeployedConfReadable)
      deployedConfAsJsValue \ "enviro" match {
        case JsString(value) => value
        case _ =>
          throw new Exception(
            s">>> Conf file [$deployedConfPath] exists and is readable, however the value for the enviro key could was not found.")
      }
    else
      defaultSysExecServerEnv.map(_.toLowerCase) match {
        case Some(local@"dev") => local
        case Some(mgmtacceptancetests@"mgmt-acceptance-tests-tests") => mgmtacceptancetests
        case Some(jenkins@"mgmt") => jenkins
        case None => throw new Exception(s"DEFAULT_SYSTEM_EXECUTION_SERVER_ENV enviro not set.")
        case unknown => throw new Exception(s"Unknown enviro [$unknown].")
      }
  }

  def isItManagementOrAcceptanceTests(env: String): Boolean = {
    env == "mgmt-acceptance-tests" || env == "mgmt"
  }

  val enviro: String = exampleExecutionEnvironment
  val targetedEnviro: String = if (isItManagementOrAcceptanceTests(enviro)) "integration" else enviro
  println(s">>> Environment is [$enviro] and the targeted environment is [$targetedEnviro].")

  case class ExampleSSLConfiguration(trustStoreLocation: String,
                                     keyStoreLocation: String,
                                     trustStorePassword: String = "examplePassword",
                                     keyStorePassword: String = "client",
                                     trustStoreType: String = "JKS",
                                     keyStoreType: String = "PKCS12")

  val exampleSSLConfiguration: Option[ExampleSSLConfiguration] = {
    enviro match {
      case "dev_local" => None
      case "integration" | "testing" | "live-production" | "mgmt-management" | "mgmt-acceptance-tests" =>
        Some(ExampleSSLConfiguration(
          trustStoreLocation = "/etc/pki/example/path/defaultTrustStore.jks",
          keyStoreLocation = "/etc/pki/example/path/defaultKeyStore.p12"
        ))
    }
  }

  object ExampleWhiteList {
    private val whitelistPath: Option[String] = enviro match {
      case "testing" => Some(s"/usr/lib/play2-scala-example/conf/whitelist/$enviro.txt")
      case "live-production" => Some(s"/usr/lib/play2-scala-example/conf/whitelist/$enviro.txt")
      case _ => None
    }
    val whitelistedEmails: Option[Set[String]] = whitelistPath map {
      Source.fromFile(_).getLines().map(_.toLowerCase.trim).toSet
    }
    println(s">>> Whitelist [${whitelistPath getOrElse s"not applicable on $enviro environment"}].")
  }

  ExampleEnvironmentSetting(enviro, defaultSysExecTestLevel)

  object ExampleMongoDB {
    val mdbWriteConcern: WriteConcern = enviro match {
      case "development-local" | "integration" | "management-jenkins" => new WriteConcern(1, 5000) // "acknowledged"
      case "testing" | "staging" | "production" => WriteConcern.majorityWriteConcern(5000, false, false)
    }

    val mongodbName = {
      enviro match {
        case "integration" => "int-mongodb"
        case _ => s"$targetedEnviro-mongodb"
      }
    }

    // MongoDB database indexes for improved database performance.
    val mdbBackgroundSetting: DBObject = ExampleMongoDBMetamorphosis.JsObjectPlay2FrameworkToDBObject(obj("background" -> true))
    // AbstractObject mongodb collection indexes.
    val abstractObjectStateIdx: DBObject = ExampleMongoDBMetamorphosis.JsObjectPlay2FrameworkToDBObject(obj("meta.state" -> 1))
    val abstractObjectIndex: DBObject = ExampleMongoDBMetamorphosis.JsObjectPlay2FrameworkToDBObject(obj("urlsList" -> 1, "meta.state" -> 1))
    abstractObjectsMDBCollection.createIndex(abstractObjectStateIdx, mdbBackgroundSetting)
    abstractObjectsMDBCollection.createIndex(abstractObjectIndex, mdbBackgroundSetting)

    val mongodbConnectionUrl: String = enviro match {
      case "integration" | "testing" | "staging" | "live" => (deployedConfAsJsValue \ "conf-secure" \ "mongodb-connect-url").as[String]
      case _ => defaultSysExecTestLevel match {
        case Some("example-docker") => "mongodb://example-mongodb-1:27017/?ssl=false"
        case _ => "127.0.0.1"
      }
    }

    def makePasswdHiddenInConnectionString(mongoDBConnectionString: String): String = {
      mongoDBConnectionString.replaceFirst(":[^/]+@", ":<hidden>@")
    }

    println(s">>> MongoDB connection url [${makePasswdHiddenInConnectionString(mongodbConnectionUrl)}].")

    val abstractObject: String = "abstractObjects"
    //    val mdbClient: MongoClient = MongoClientFactory.createMongoClient(mongodbConnectionUrl)
    val mdbClient: MongoClient = new MongoClient(mongodbConnectionUrl)
    val mongodbDatabase: DB = mdbClient.getDB(mongodbName)
    val abstractObjectsMDBCollection: DBCollection = mongodbDatabase.getCollection(abstractObject)
    val allMDBCollections: Seq[DBCollection] = Seq(abstractObjectsMDBCollection)
  }

  object ExampleComponentConfigObject {
    val exampleUri: String = targetedEnviro match {
      case _ if isItManagementOrAcceptanceTests(enviro) || enviro == "local-development" => {
        defaultSysExecTestLevel match {
          case Some("example-docker") => "https://example.integration.app.com"
          case _ => s"$defaultStubbedServerUrl/example/api/path"
        }
      }
      case "production-live" => s"https://example.live.app.com"
      case environment => s"https://example.$environment.app.com"
      case _ => "http://localhost:8080"
    }
    val exampleScheme = StringUtils.substringBefore(exampleUri, "://")
    val exampleHost = StringUtils.substringAfter(exampleUri, "://")
    val examplePath = "/example/path"
  }


  object ExampeAWSS3BucketConfObj {
    def exampleS3Client: AmazonS3Client = {
      val exampleS3credentials: InstanceProfileCredentialsProvider = new InstanceProfileCredentialsProvider
      new AmazonS3Client(exampleS3credentials, new ClientConfiguration) {
        setEndpoint("example-s3-eu-west-1.amazonaws.com")
      }
    }

    val exampleS3Bucket1: String = s"example-s3-bucket-1-$targetedEnviro"
    val publicBucketUrl = enviro match {
      case "production" => "http://example.files.production.example.com"
      case "integration" | "testing" | "staging" => "http://example.files.example.com"
      case "local-development" | "management" => "http://example.files.example.com"
    }
  }

  object ExampleApiConfigurationObject {

    if (enviro == "local-development") {
      System.setProperty("com.ning.http.client.AsyncHttpClientConfig.useProxyProperties", "true")
    }

    val exampleKey = "asd123"
    val exampleSecret = "456qwe"
    private lazy val exampleDefaultBearerToken: String = {
      new BASE64Encoder().encode(s"$exampleKey:$exampleSecret".getBytes("utf-8")).replaceAll(sys.props("line.separator"), "")
    }
    val exampleTokenUri: String = s"https://example.com/oauth2/token"

    lazy val exampleAccessToken: String = {
      val exampleAccessTokenHeaders: Seq[(String, String)] = Seq(
        "Content-Type" -> "application/x-www-form-urlencoded;charset=UTF-8",
        "Authorization" -> s"Basic $exampleDefaultBearerToken"
      )

      //|||oauth
      //|||oauth2
      //|||http oauth |||https oauth
      //|||http oauth2 |||https oauth2
      //|||grant_type=client_credentials
      //https://stackoverflow.com/questions/34842895/difference-between-grant-type-client-credentials-and-grant-type-password-in-auth
      val examplePayload: String = s"grant_type=client_credentials"

      val exampleTokenAsFutureOfString: Future[String] = Http(url(exampleTokenUri) <:< exampleAccessTokenHeaders << examplePayload) map {
        //        case Response(200, responsePayload) => (parse(responsePayload) \ "access_token").as[String]
        //        case Response(code, _) => throw new Exception(s"Error response while trying to get access token with http response code [$code].")
        case _ => throw new Exception("Error")
      }
      Await.result(exampleTokenAsFutureOfString, 10.seconds)
    }

    val headers: Seq[(String, String)] = enviro match {
      case "local-development" | "management" => Seq()
      case _ => Seq("Authorization" -> s"Bearer $exampleAccessToken")
    }
  }

  object ExampleAWSSQSConfObj {
    val exampleAWSSQSClient: AmazonSQSClient = {
      enviro match {
        case "local-development" | "management-mgmt" =>
          println(s">>> Using dummy AWS SQS client on [$enviro] env.")
          new ExampleScalaSQSClientDummy
        case _ => new AmazonSQSClient {
          setEndpoint("example-sqs.eu-west-1.amazonaws.com")
        }
      }
    }

    val exampleSQSURI = {
      enviro match {
        case "prod" => "https://example-sqs.eu-west-1.amazonaws.com/[INSERT-AWS-ACC-NUM-HERE]/prod-sqs-name"
        case "staging" => "https://example-sqs.eu-west-1.amazonaws.com/[INSERT-AWS-ACC-NUM-HERE]/staging-sqs-name"
        case "testesting" => "https://example-sqs.eu-west-1.amazonaws.com/[INSERT-AWS-ACC-NUM-HERE]/testing-sqs-name"
        case "integration-int" => "https://example-sqs.eu-west-1.amazonaws.com/[INSERT-AWS-ACC-NUM-HERE]/integration-sqs-name"
        case "local-dev" | "management-mgmt" => ""
      }
    }
  }

  object ExampleAWSCloudWatchConfObj {
    val enabled: Boolean = Seq("prod", "staging", "testing", "integration") contains enviro
  }

  object ExampleAWSSNSConfObj {
    val exampleSNSArn: String = {
      enviro match {
        case "prod" => "arn:aws:sns:eu-west-1:[INSERT-AWS-ACC-NUM-HERE]:prod-sns-topic-name"
        case "staging" => "arn:aws:sns:eu-west-1:[INSERT-AWS-ACC-NUM-HERE]:staging-sns-topic-name"
        case "testing" => "arn:aws:sns:eu-west-1:[INSERT-AWS-ACC-NUM-HERE]:testing-sns-topic-name"
        case "integration" => "arn:aws:sns:eu-west-1:[INSERT-AWS-ACC-NUM-HERE]:integration-sns-topic-name"
        case "local-dev" | "management-mgmt" => ""
      }
    }

    val exampleSNSCredentials: InstanceProfileCredentialsProvider = new InstanceProfileCredentialsProvider()
    val exampleAWSSNSRegion = "example-sns.eu-west-1.amazonaws.com"

    lazy val exampleAWSSNSClient: AmazonSNSClient = {
      enviro match {
        case "prod" | "staging" | "testing" | "integration" =>
          new AmazonSNSClient(exampleSNSCredentials, new ClientConfiguration().withMaxErrorRetry(3)) {
            setEndpoint(exampleAWSSNSRegion)
          }
        case "local-development" | "management" => ExampleAWSSNSClientDummy()
      }
    }

    def apply() = exampleAWSSNSClient
  }

}

