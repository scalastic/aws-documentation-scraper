package io.scalastic.aws

import scala.util.{Failure, Success}

object AwsDocumentation extends AwsWebScraper with Serializer with IO with App {

  val rootDocumentationUrl: String = "https://docs.aws.amazon.com"

  val documentationPath: String = "./data/"
  val rootDocumentationFilename: String = "root-documentation"
  val fullDocumentationFilename: String = "full-documentation"

  // 1. Get AWS root documentation entries
  val rootDocumentation: ujson.Value = deserialize(rootDocumentationFilename, classOf[ujson.Value]) match {
    case Success(s: ujson.Value) => s
    case Failure(f) => {
      println(f)
      val doc = extractDocumentation(rootDocumentationUrl)
      serialize(rootDocumentationFilename, doc)
      doc
    }
  }

  // 2. Complete documentation with href content from root documentation
  val fullDocumentation: ujson.Value = deserialize(fullDocumentationFilename, classOf[ujson.Value]) match {
    case Success(s: ujson.Value) => s
    case Failure(f) => {
      println(f)
      completeRootDocumentation(rootDocumentation)
      serialize(fullDocumentationFilename, rootDocumentation)
      rootDocumentation
    }
  }

  // 3. Write resulting JSON to file
  write(fullDocumentationFilename, fullDocumentation)

}


