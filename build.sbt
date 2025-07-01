ThisBuild / organization := "Keyla-TTT"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.0"

scalacOptions ++= Seq(
  "-Wunused:all",
  "-Wconf:cat=unused:info"
)
enablePlugins(ScalafixPlugin)
enablePlugins(ScalafmtPlugin)
enablePlugins(GitPlugin)

val installGitHook = taskKey[Unit]("Install git pre-commit hook")
installGitHook := {
  val hookFile = baseDirectory.value / ".git" / "hooks" / "pre-commit"
  IO.write(hookFile,
    """#!/bin/sh
      |echo "Executing scalafmt and scalafix..."
      |files=$(git diff --cached --name-only --diff-filter=ACMR | grep ".*\\.scala$")
      |if [ -n "$files" ]; then
      |  sbt "scalafmtCheck; scalafixAll --check"
      |  if [ $? -ne 0 ]; then
      |    echo "Code style check failed. Running scalafmt and scalafix..."
      |    sbt "scalafmtAll; scalafixAll"
      |    git add $files
      |  fi
      |fi
      |""".stripMargin)
  hookFile.setExecutable(true)
  println("Git pre-commit hook installed successfully")
}

inThisBuild(List(
  scalaVersion := "3.7.0",
     semanticdbEnabled := true,
     semanticdbVersion := scalafixSemanticdb.revision
))
Global / onChangedBuildSource := ReloadOnSourceChanges

val tapirVersion = "1.11.35"
val http4sVersion = "0.23.32"
val catsEffectVersion = "3.6.1"
val jsoniterVersion = "2.6.4"

lazy val root = (project in file("."))
  .settings(
    name := "API-Scala",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.testcontainers" % "mongodb" % "1.21.2" % Test,
      "de.flapdoodle.embed" % "de.flapdoodle.embed.mongo" % "4.20.1" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
      "org.http4s" %% "http4s-blaze-client" % "0.23.17" % Test,
      "com.softwaremill.sttp.client3" %% "core" % "3.9.8" % Test,
      "com.softwaremill.sttp.client3" %% "cats" % "3.9.8" % Test,
      "com.softwaremill.sttp.client3" %% "jsoniter" % "3.9.8" % Test,
      "com.softwaremill.sttp.client3" %% "http4s-backend" % "3.9.8" % Test,

      // MongoDB driver
      "org.mongodb" % "mongodb-driver-sync" % "5.5.1",
      "org.mongodb" % "bson" % "5.5.0",

      // Cats Effect
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.typelevel" %% "cats-core" % "2.12.0",

      // HTTP4s
      "org.http4s" %% "http4s-blaze-server" % "0.23.17",

      // Tapir with HTTP4s
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % "1.11.35",
      "com.softwaremill.sttp.model" %% "core" % "1.7.11",
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.36.6",

      // Use the "provided" scope instead when the "compile-internal" scope is not supported
      "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.36.6" % "compile-internal",
      // Logging
      "org.typelevel" %% "log4cats-slf4j" % "2.7.0",
      "ch.qos.logback" % "logback-classic" % "1.2.12"
    ),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.19" % Test,
    libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "3.0.0",
  )
