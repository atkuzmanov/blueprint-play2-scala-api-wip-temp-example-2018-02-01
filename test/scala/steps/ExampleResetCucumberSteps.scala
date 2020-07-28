package steps

import cucumber.api.scala.ScalaDsl

class ExampleResetCucumberSteps extends ScalaDsl {
  Before { _ =>
    ExampleDefaultMockServer.exampleMockServer.reset()
  }
}
