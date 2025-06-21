

ThisBuild / organization := "Keyla-TTT"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.0"

Test / testOptions += Tests.Filter(name => !name.contains("Docker"))

scalacOptions ++= Seq(
  "-Wunused:all",
  "-Wconf:cat=unused:info"
)
enablePlugins(ScalafixPlugin)
enablePlugins(ScalafmtPlugin)

inThisBuild(List(
  scalaVersion := "3.7.0",
     semanticdbEnabled := true,
     semanticdbVersion := scalafixSemanticdb.revision
))
Global / onChangedBuildSource := ReloadOnSourceChanges


lazy val root = (project in file("."))
  .settings(
    name := "API-Scala",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.testcontainers" % "mongodb" % "1.21.1" % Test,
      "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "3.5.3" % Test,

      // MongoDB driver
      "org.mongodb" % "mongodb-driver-sync" % "5.5.1", // Driver Java sincrono
      "org.mongodb" % "bson" % "5.5.0"// Driver Java sincrono
    ),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "3.0.0",
  )
