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
