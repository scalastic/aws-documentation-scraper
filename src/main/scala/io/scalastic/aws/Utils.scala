package io.scalastic.aws

import io.scalastic.aws.AwsDocumentation.{extractJson, rootDocumentationUrl}
import ujson.{Arr, Obj, Str, Value}

import scala.util.control.Breaks.{break, breakable}
import scala.util.{Failure, Success, Try}

trait Utils {

  def enhancer(jsonData: Value, parentKey: String): Value = jsonData match {
    case a: Arr => manageId(parentKey, a)
    case o: Obj => manageHref(parentKey, o)
    case z => z
  }

  private def manageHref(parentKey: String, jsonObject: Obj): Arr = {

    jsonObject.obj.map {
      case (k, hrefPath: Str) if isAwsHrefPath(k, hrefPath) => {
        jsonObject.obj(k) = setAwsAbsoluteUrl(hrefPath)
        addInfo(jsonObject, jsonObject.obj(k).str)
        enhancer(hrefPath, parentKey + "-" + k)
      }
      case (k, pdfPath: Str) if k == "pdf" => {
        jsonObject.obj(k) = setAwsAbsoluteUrl(pdfPath, jsonObject)
        enhancer(pdfPath, parentKey + "-" + k)
      }
      case (k, v) => enhancer(v, parentKey + "-" + k)
    }
  }

  private def setAwsAbsoluteUrl(s: Str): Str = {
    Str(rootDocumentationUrl + s.str)
  }

  private def setAwsAbsoluteUrl(s: Str, jsonObject: Obj): Str = {
    val url = getURLwoFilename(jsonObject.obj.get("href").get.str)
    Str(url + s.str)
  }

  private def getURLwoFilename(url: String): String = {
    url.substring(0, url.lastIndexOf('/') + 1)
  }

  private def addInfo(jsonObject: Obj, hrefPath: String) = {

    val infoPath: String = getURLwoFilename(hrefPath) + "meta-inf/guide-info.json"

    Try {
      extractJson(infoPath)
    } match {
      case Success(s) => {
        for (i <- s.obj.keys) {
          breakable {
            val newValue: String = s.obj.get(i).get.str

            if (newValue.startsWith("Insert " + i)) break

            if (i == "subtitle") jsonObject.obj("title") = newValue
            else if (i != "title") jsonObject.obj(i) = newValue
          }
        }
      }
      case Failure(f) => println(f.getMessage)
    }
  }

  private def isAwsHrefPath(k: String, s: Str): Boolean = {
    k == "href" & s.str.startsWith("/")
  }

  private def manageId(parentKey: String, jsonArray: Arr): Arr = {

    jsonArray.arr.zipWithIndex.map { case (s, i) =>
      if (jsonArray(i).isInstanceOf[Obj])
        jsonArray(i)("id") = parentKey + i.toString
      else
        jsonArray(i)
      enhancer(jsonArray(i), parentKey + i.toString)
    }
  }

}
