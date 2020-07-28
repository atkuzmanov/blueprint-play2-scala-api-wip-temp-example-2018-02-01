package steps

import play.api.libs.json.JsString
import _root_.util.ExampleTestUtilities
import play.api.libs.ws.WS

class ExampleHttpAPISteps extends ExampleApiSteps with ExampleTestUtilities {
  When("""^I request the status of the example test http/https rest api$""") { () =>
    exampleAwaitResponseFuture(WS.url(s"$exampleFoundationURI/status").get())
  }

  Then("""^the example test http/https rest api response status code is (\d+)$""") { (status: Int) =>
    withClue(exampleWSResponse.body) {
      assert(exampleWSResponse.status == status)
    }
  }

  Then("""^the http/https rest api response is not found$""") { () =>
    withClue(exampleWSResponse.body) {
      assert(exampleWSResponse.status == 404)
      (exampleWSResponse.json \ "example-msg") mustBe JsString("Not found.")
    }
  }

  Then("""^the example test http/https rest api response payload JSON is the same as the JSON in fixture located on path "(.*?)"$""") { (fixturePath: String) =>
    exampleWSResponse.json mustBe exampleReadFixtureAsJson(fixturePath)
  }

  private def exampleContentTypeChecker(contentType: String): Option[Unit] = {
    exampleRespContentType map (_ must include(contentType)) orElse fail(s"Error Content-Type header doesn't have [$contentType].")
  }
  Then("""^the example test http/https rest api response has Content-Type header values "(.*?)" and "(.*?)"$""") { (conType1: String, conType2: String) =>
    exampleWSResponse.status mustBe (200)
    exampleContentTypeChecker(conType2)
    exampleContentTypeChecker(conType1)
  }
}
