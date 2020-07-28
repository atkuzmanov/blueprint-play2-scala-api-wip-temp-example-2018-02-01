package example.play2

// Example package object
package object scala_app {
  type Identifier = String
  class ExampleExceptionOfErroneousState(message: String) extends Exception(message)
  class ExampleExceptionForFailedProcessing extends Exception
}
