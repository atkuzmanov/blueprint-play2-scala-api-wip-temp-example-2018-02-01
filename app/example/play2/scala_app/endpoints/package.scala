package example.play2.scala_app

package object endpoints {
  object ExampleCustomHeaders {
    val ExampleCustomHTTPHeader1 = "Z-Example-custom-http-header-1"
  }
  class ExampleCustomHTTPHeaderException(message: String) extends Exception(message)
}
