import Dependencies._
import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion     := "2.13.5"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "io.scalastic"
ThisBuild / organizationName := "scalastic"

lazy val root = (project in file("."))
  .settings(
    name := "aws-doc-scraper",
    resolvers += "Rally Health" at "https://dl.bintray.com/rallyhealth/maven",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.softwaremill.sttp.client3" %% "core" % "3.3.5",
      "com.lihaoyi" %% "upickle" % "1.3.15",
      "org.json" % "json" % "20210307",
    )
)


// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
