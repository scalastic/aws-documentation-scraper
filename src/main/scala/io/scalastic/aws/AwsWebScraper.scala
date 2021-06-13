package io.scalastic.aws

import io.scalastic.aws.AwsDocumentation.rootDocumentationUrl
import sttp.client3.{HttpURLConnectionBackend, UriContext, basicRequest}
import ujson.Value.InvalidData

import scala.util.{Failure, Success, Try}

trait AwsWebScraper extends Model {

  def completeRootDocumentation(v: ujson.Value): ujson.Value = v match {
    case a: ujson.Arr =>
      a.arr.map(completeRootDocumentation)
    case o: ujson.Obj =>
      o.obj.mapValuesInPlace {
        case (k, v) if k == "href" => extractDocumentation(v.str)
        case (k, v) => completeRootDocumentation(v)
      }
    case s: ujson.Str => s
    case n: ujson.Num => n
    case _ => Nil
  }

  def extractDocumentation(url: String = rootDocumentationUrl): ujson.Value = {

    val pageUrl: String = if (url.startsWith("http")) url else rootDocumentationUrl + url

    val page: Page = PageFactory.build(uri"$pageUrl")
    page.extract
  }

  def extractJson(url: String): ujson.Value = {

    basicRequest
      .get(uri"$url")
      .send(HttpURLConnectionBackend())
      .body match {
      case Right(s) => ujson.read(s)
      case Left(f) => throw InvalidData(ujson.read("{}"), "No JSON found on " + url)
    }
  }

  private def extractField(jsonData: ujson.Value, fieldsList: List[String]): ujson.Value = {

    if (fieldsList.isEmpty) return jsonData

    Try {
      jsonData(fieldsList.head)
    } match {
      case Success(s) => return extractField(s, fieldsList.tail)
      case Failure(f) => return "Empty " + fieldsList.head
    }
  }
}
