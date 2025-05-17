ThisBuild / organization := "Keyla-TTT"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.0"

enablePlugins(ScalafixPlugin)
enablePlugins(ScalafmtPlugin)

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .settings(
    name := "API-Scala",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test


    /* bash
    *
    * GITHUB_TOKEN=tuo-token_github sbt publish*/

    /* //Configurazione per pubblicare su GitHub Packages
    publishTo := Some("GitHub Packages" at "https://maven.pkg.github.com/Keyla-TTT/Keyla-API"),
    publishMavenStyle := true,
    credentials += Credentials(
      "GitHub Package Registry",
      "maven.pkg.github.com",
      sys.env.getOrElse("GITHU_ACTOR",""),
      sys.env.getOrElse("GITHUB_TOKEN","")
    )*/
  )
