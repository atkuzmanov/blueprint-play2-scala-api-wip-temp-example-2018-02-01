package example.play2.scala_app.persistence.parsers

import org.apache.commons.lang3.StringEscapeUtils._
import play.api.libs.json.{JsObject, JsValue, Json}

// import scala.annotation.tailrec
import scala.util.matching.Regex

// keep, commented out on purpose
// BodyElement, BodyListItem, Link ... etc. are content objects or case classes which need to be implemented.
object ExampleHtmlBodyParser extends ExampleHtmlBodyParserHelper {
  override val examplePreprocessor: ExampleParserPreprocessor = exampleFixKnown compose exampleRemoveUnknown

  //  override def defaultEntryPoint: Parser[List[BodyElement]] = defaultBodyElement.*
  override def defaultEntryPoint: Parser[List[JsObject]] = defaultBodyElement.*

  //  def defaultListItem: Parser[BodyListItem] = {
  def defaultListItem: Parser[JsObject] = {
    "<li>" ~> defaultInlineElement.* <~ "</li>" ^^ { defaultInline => BodyListItem(defaultInline.flatten) }
  }

  // made up dummy example
  def BodyList(a: Any): JsObject = Json.obj("a" -> Json.parse(s"{$a}"))

  // made up dummy example
  def BodyListItem(a: Any): JsObject = Json.obj("a" -> Json.parse(s"{$a}"))

  //  package example.default
  //  case class BodyList(val ordering : scala.Predef.String, val defaultListOfBodyItems : scala.List[example.default.BodyListItem]) extends scala.AnyRef with scala.Product with scala.Serializable {
  //  }
  //  def defaultOrderedList: Parser[BodyList] = {
  def defaultOrderedList: Parser[JsObject] = {
    "<ol>" ~> defaultListItem.* <~ "</ol>" ^^ {
      BodyList("body_list_ordered", _)
    }
  }

  //  def defaultUnorderedList: Parser[BodyList] = {
  def defaultUnorderedList: Parser[JsObject] = {
    "<ul>" ~> defaultListItem.* <~ "</ul>" ^^ {
      BodyList("body_list_unordered", _)
    }
  }

  private val exampleFamiliarTags: List[Regex] = {
    List("a", "p", "b", "i", "ul", "ol", "li", "span").flatMap(tag => List(s"<$tag>".r, s"<$tag\\s+.*?>".r, s"</$tag>".r))
  }

  private val exampleTagFixers: List[(Regex, String)] = {
    List("p", "b", "i", "ul", "ol", "li").flatMap(tag =>
      List(s"<$tag>".r -> s"<$tag>", s"<$tag\\s+.*?>".r -> s"<$tag>", s"</$tag>".r -> s"</$tag>"))
  }

  private val exampleRemoveUnknown: ExampleParserPreprocessor = { (entry: String) =>
    "<.+?>".r.replaceSomeIn(entry, { m => if (exampleFamiliarTags.exists(_.findFirstIn(m.matched).isDefined)) None else Some("") })
  }


  private val exampleFixKnown: ExampleParserPreprocessor = { (entry: String) =>
    "<.+?>".r.replaceSomeIn(entry, { m => exampleTagFixers.find(_._1.findFirstIn(m.matched).isDefined).map(_._2) })
  }


  // package  example //.InlineElement
  //  sealed trait InlineElement extends scala.AnyRef {
  //  }
  //  def defaultInlineParagraph: Parser[List[InlineElement]] = {
  def defaultInlineParagraph: Parser[List[JsObject]] = {
    "<p>" ~> defaultInlineElement.* <~ "</p>" ^^ {
      _.flatten
    }
  }

  // made up dummy example
  def Paragraph(a: Any): JsObject = Json.obj("a" -> Json.parse(s"{$a}"))

  //  def defaultParagraph: Parser[Paragraph] = {
  def defaultParagraph: Parser[JsObject] = {
    "<p>" ~> defaultInlineElement.* <~ "</p>" ^^ { defaultInlineElems => Paragraph(defaultInlineElems.flatten) }
  }

  //  def defaultBodyElement: Parser[BodyElement] = {
  def defaultBodyElement: Parser[JsObject] = {
    defaultParagraph | defaultOrderedList | defaultUnorderedList
  }

  def defaultText: Parser[String] = (defaultEscaped | defaultUnescaped).+ ^^ {
    _.mkString
  }

  // made up dummy example
  def InlineText(a: Any): JsObject = Json.obj("a" -> Json.parse(s"{$a}"))

  //  def defaultInlineText: Parser[List[InlineText]] = {
  def defaultInlineText: Parser[List[JsObject]] = {
    defaultText ^^ { exampleTxt => List(InlineText(exampleTxt)) }
  }

  // package example
  // case class Italic(val inlineElement : example.InlineElement) extends scala.AnyRef with example.InlineElement with scala.Product with scala.Serializable {
  // }
  // made up dummy example
  def Italic(a: Any): JsObject = Json.obj("a" -> Json.parse(s"{$a}"))
//  def defaultItalic: Parser[List[Italic]] = {
  def defaultItalic: Parser[List[JsObject]] = {
    "<i>" ~> defaultInlineElement.* <~ "</i>" ^^ {
      _.flatten map Italic
    }
  }

  // package  example //.Bold
  //  case class Bold(val inlineElement : example.rendering.InlineElement) extends scala.AnyRef with example.InlineElement with scala.Product with scala.Serializable {
  //  }
//  def defaultBold: Parser[List[Bold]] = {
  // made up dummy example
  def Bold(a: Any): JsObject = Json.obj("a" -> Json.parse(s"{$a}"))
  def defaultBold: Parser[List[JsObject]] = {
    "<b>" ~> defaultInlineElement.* <~ "</b>" ^^ {
      _.flatten map Bold
    }
  }

//  def defaultInlineElement: Parser[List[InlineElement]] = {
  def defaultInlineElement: Parser[List[JsObject]] = {
//    defaultBold | defaultItalic | defaultInlineText | defaultLink | defaultInlineParagraph | defaultSpan
    defaultBold | defaultItalic | defaultInlineText | defaultInlineParagraph | defaultSpan
  }

  def defaultSpanCaption: Parser[String] = (defaultItalic | defaultInlineText | defaultBold).* <~ "</span>" ^^ {
//    _.flatten.foldLeft("")(_ + defaultUnformatted(_))
    _.flatten.foldLeft("")(_ + _)
  }

  def defaultSpanLanguage: Parser[String] = """<span\s+.*?dir="ltr"\s+.*?lang="""".r ~> defaultEscapedHref <~ """".*?>""".r

  // made up dummy example
  def Span(a: Any): JsObject = Json.obj("a" -> Json.parse(s"{$a}"))
//  def defaultSpan: Parser[List[Span]] = {
  def defaultSpan: Parser[List[JsObject]] = {
    defaultSpanLanguage ~ defaultSpanCaption ^^ {
      case defaultLanguage ~ defaultCaption =>
        List(Span(defaultLanguage.split("-").head, InlineText(defaultCaption)))
    }
  }

//  @tailrec def defaultUnformatted(defaultElement: InlineElement): String = {
//    (defaultElement: @unchecked) match {
//      case Italic(defaultedNested) => defaultUnformatted(defaultedNested)
//      case Bold(defaultedNested) => defaultUnformatted(defaultedNested)
//      case InlineText(defaultText) => defaultText
//    }
//  }

  def defaultUnescaped: Parser[String] = "[^&<>]+".r

  def defaultEscapedHref: Parser[String] = {
    """[^"]+""".r ^^
      unescapeHtml4
  }

  def defaultEscaped: Parser[String] = {
    "&[^;]+;".r ^^
      unescapeHtml4
  }


//  def defaultCaptionOfLink: Parser[String] = (defaultItalic | defaultBold | defaultInlineText).* <~ "</a>" ^^ {
//    _.flatten.foldLeft("")(_ + defaultUnformatted(_))
//  }

  def defaultUrlLink: Parser[String] = """<a\s+.*?href="""".r ~> defaultEscapedHref <~ """".*?>""".r

  //  package example
  //  case class Link(val url: scala.Predef.String, val caption: scala.Predef.String, val defaultOtherUrls: List[Url]) extends scala.AnyRef with example.play2.rendering.domain.InlineElement with scala.Product with scala.Serializable {
  //    def isInternal : scala.Boolean = { /* compiled code */ }
  //  }
//  def defaultLink: Parser[List[Link]] = {
//    defaultUrlLink ~ defaultCaptionOfLink ^^ { case defaultUrl ~ defaultCaption =>
//      List(Link(defaultUrl = defaultUrl, caption = defaultCaption, defaultAltText = defaultCaption,
//        defaultOtherUrls = List(Url(defaultUrl = defaultUrl))))
//    }
//  }
}
