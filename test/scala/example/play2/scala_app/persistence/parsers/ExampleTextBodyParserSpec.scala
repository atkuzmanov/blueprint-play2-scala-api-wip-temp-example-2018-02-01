package test.scala.example.play2.scala_app.persistence.parsers

import org.scalatest.{FlatSpec, MustMatchers}

class ExampleTextBodyParserSpec extends FlatSpec with MustMatchers {
  import example.play2.scala_app.persistence.parsers.ExampleHtmlBodyParser.{apply => parse}

  "Example HTML body parser" should "throw an exception when a mandatory attribute, such as 'dir' is missing from the span" in {
    val exampleTestInput: String = """<p><span lang="en-gb">example span text</span></p>"""
    intercept[ExampleHtmlBodyParserHelperException] {parse(exampleTestInput)}
  }

  it should "manage to process paragraph which is empty" in {
    val exampleTestInput: String = "<p></p>"
    parse(exampleTestInput) mustBe List(
      Paragraph(
        List()))
  }

  it should "manage formatting which is double nested" in {
    val exampleTestInput: String = "<p><b><i>bold italic</i></b></p>"
    parse(exampleTestInput) mustBe List(
      Paragraph(
        List(Bold(Italic(InlineText("bold italic"))))))
  }

  it should "manage to process a sequence of adjacent elements with nested formatting" in {
    val exampleTestInput: String = "<p><b><i>example_bold example_italic</i></b>, example_unformatted, <b>bold</b><i>example_italic</i>, example_unformatted</p>"
    parse(exampleTestInput) mustBe List(
      Paragraph(
        List(
          Bold(Italic(InlineText("example_bold example_italic"))),
          InlineText(", example_unformatted, "),
          Bold(InlineText("example_bold")),
          Italic(InlineText("example_italic")),
          InlineText(", example_unformatted"))))
  }

  it should "keep whitespaces which are leading and/or trailing" in {
    val exampleTestInput: String = "<p> <b> <i> example_italic example_bold </i></b> example_unformatted <b> example_italic</b><i> example_bold</i>, example_unformatted </p>"
    parse(exampleTestInput) mustBe List(
      Paragraph(
        List(
          InlineText(" "),
          Bold(InlineText(" ")),
          Bold(Italic(InlineText(" example_italic example_bold "))),
          InlineText(" example_unformatted "),
          Bold(InlineText(" example_italic")),
          Italic(InlineText(" example_bold")),
          InlineText(", example_unformatted "))))
  }

  it should "be able to process a mix a escaped and unescaped input" in {
    val exampleTestInput: String =
      """<p>&iquest;&copy;</p>""" +
      """<p>&lt;&amp;&gt; \,"&nbsp;.</p>""" +
      """<p>&example_unknown;</p>"""
    parse(exampleTestInput) mustBe List(
      Paragraph(
        List(
          InlineText("""¿©"""))),
      Paragraph(
        List(
          InlineText("""<&> \," ."""))),
      Paragraph(
        List(
          InlineText("""&example_unknown;"""))))
  }
}



