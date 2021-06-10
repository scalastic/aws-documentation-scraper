package io.scalastic.aws

import org.json.XML
import sttp.client3.{HttpURLConnectionBackend, basicRequest}
import sttp.model.Uri
import ujson.Value

import java.net.URLDecoder
import java.nio.charset.Charset
import scala.util.{Failure, Success, Try}

trait Model {

  val sort: Option[String] = None
  val query: String = "http language:scala"
  val xmlVariable = "window.awsdocs.landingPageXml"

  trait Html {

    val uri: Uri

    val htmlContent: String = {
      val request = basicRequest.get(uri)
      val backend = HttpURLConnectionBackend()
      val response = request.send(backend)
      response.body.toString
    }
  }

  trait Xml extends Html {

    val xmlContent: String = findXml()

    def findXml(innerVariable: String = "window.awsdocs.landingPageXml"): String = {
      val strPattern = innerVariable + " = '(.*?)'"
      val pattern = strPattern.stripMargin.r
      val encodedContent = pattern.findFirstIn(htmlContent)

      URLDecoder.decode(encodedContent.getOrElse(""), Charset.defaultCharset())
    }
  }

  trait Json extends Xml {

    var jsonContent: ujson.Value = {
      val cleanContent = xmlContent.toString.replaceAll("\n", "")
      try {
        ujson.read(XML.toJSONObject(cleanContent).toString)
      } catch {
        case ignore: NumberFormatException => ujson.read("{\"EMPTY\":\"EMPTY\"}")
      }
    }
  }

  trait Page {
    val category: String

    def extractField(jsonData: ujson.Value, fieldsList: List[String]): ujson.Value = {
      if (fieldsList.isEmpty) return jsonData
      Try {
        jsonData(fieldsList.head)
      } match {
        case Success(s) => return extractField(s, fieldsList.tail)
        case Failure(f) => return "Empty " + fieldsList.head
      }
    }

    def filter: ujson.Value

    def extract: ujson.Value

    protected def getCategory = category
  }

  class MainPage(val uri: Uri) extends Page with Json {
    override val category = "main"

    override def extract: Value = ujson.Obj(
      "title" -> extractField(filter, List("title")),
      "subtitle" -> extractField(filter, List("service-categories", "title")),
      "abstract" -> extractField(filter, List("abstract")),
      "panels" -> extractField(filter, List("service-categories", "tiles", "tile"))
    )

    override def filter: Value = jsonContent("main-landing-page")
  }

  class SubPage(val uri: Uri) extends Page with Json {
    override val category = "sub"

    override def extract: Value = ujson.Obj(
      "title" -> extractField(filter, List("title")),
      "short-title" -> extractField(filter, List("titleabbrev")),
      "abstract" -> extractField(filter, List("abstract")),
      "sections" -> extractField(filter, List("main-area", "sections", "section"))
    )

    override def filter: Value = jsonContent("landing-page")
  }

  class ResourcePage(val uri: Uri) extends Page with Json {
    override val category = "resource"

    override def filter: Value = jsonContent("main-landing-page")

    override def extract: Value = ???
  }

  class UnknownPage(val uri: Uri) extends Page with Json {
    override val category = "unknown"

    override def filter: Value = ujson.read("{\"empty\":\"empty\"}")

    override def extract: Value = ujson.Obj()
  }

  object PageFactory {

    def build(uri: Uri): Page = {
      TestPage(uri) match {
        case c if c.jsonContent.toString().startsWith("{\"main-landing-page\":{\"general_resources\"") => new MainPage(uri)
        case c if c.jsonContent.toString.startsWith("{\"landing-page\"") => new SubPage(uri)
        case c if c.jsonContent.toString.startsWith("{\"main-landing-page\"") => new ResourcePage(uri)
        case c => new UnknownPage(uri)
      }
    }

    case class TestPage(uri: Uri) extends Json
  }

}

