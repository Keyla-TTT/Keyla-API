

ThisBuild / organization := "Keyla-TTT"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.7.0"

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

lazy val startupTransition: State => State = "conventionalCommits" :: _
lazy val root = (project in file("."))
  .settings(
    name := "API-Scala",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.18" % Test,
    Global / onLoad ~= (_ andThen ("conventionalCommits" :: _))
  )
