package example.play2.scala_app

object ExampleEnvironmentSetting {
  def apply(enviro: String, testingLevel: Option[String]) {
    if (Seq("integration", "testing", "staging", "production").contains(enviro) || ExampleEnviroConf.isItManagementOrAcceptanceTests(enviro)) {
      System.setProperty("javax.net.ssl.keyStoreType", "PKCS12")
      System.setProperty("javax.net.ssl.trustStore", "/etc/pki/example/path/to/exampleTrustStore.jks")
      System.setProperty("javax.net.ssl.keyStoreLocation", "/etc/pki/tls/private/example/path/to/exampleKeyStoreLocation.p12")
      System.setProperty("javax.net.ssl.keyStore", "/etc/pki/tls/private/example/path/to/exampleKeyStore.p12")
      System.setProperty("javax.net.ssl.keyStorePassword", "exampleKeyStorePassword")
    }
    else if (Seq("development-local").contains(enviro)) {
      testingLevel match {
        case Some("docker") => {
          System.setProperty("javax.net.ssl.keyStoreLocation", "/etc/pki/example/path/to/exampleKeyStoreLocation.p12")
          System.setProperty("javax.net.ssl.keyStore", "/etc/pki/example/path/to/exampleKeyStore.p12")
          System.setProperty("javax.net.ssl.trustStore", "/etc/pki/example/path/to/exampleTrustStore")
        }
        case _ => {
          System.setProperty("javax.net.ssl.keyStore", "/etc/pki/example/path/to/exampleKeyStore.p12")
          System.setProperty("javax.net.ssl.trustStore", "/etc/pki/example/path/to/exampleTrustStore.jks")
          System.setProperty("javax.net.ssl.keyStoreLocation", "/etc/pki/example/path/to/exampleKeyStoreLocation.p12")
        }
      }
      System.setProperty("javax.net.ssl.keyStoreType", "PKCS12")
      System.setProperty("javax.net.ssl.keyStorePassword", "exampleKeyStorePassword")
    }
  }
}
