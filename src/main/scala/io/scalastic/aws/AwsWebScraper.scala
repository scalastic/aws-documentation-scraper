package io.scalastic.aws

import io.scalastic.aws.AwsDocumentation.rootDocumentationUrl
import sttp.client3.UriContext

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
    val pageUri = uri"$pageUrl"

    val page: Page = PageFactory.build(pageUri)
    page.extract
  }

  /**
   * @TODO :
   *  - Manage href-only tag and then retrieve info from [https://docs.aws.amazon.com + href + meta-inf/guide-info.json]
   */
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
