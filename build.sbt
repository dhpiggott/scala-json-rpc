import sbt.Keys._

scalaVersion in ThisBuild := "2.11.8"

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "com.dhpcs",
  version := "1.2.0"
)

lazy val playJsonRpc = project.in(file("play-json-rpc"))
  .settings(commonSettings)
  .settings(Seq(
    name := "play-json-rpc",
    publishArtifact in Test := true,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.10",
      "org.scalatest" %% "scalatest" % "3.0.0" % "test"
    )
  ))

lazy val sample = project.in(file("sample"))
  .settings(commonSettings)
  .settings(Seq(
    name := "play-json-rpc-sample"
  ))
  .dependsOn(playJsonRpc)
  .dependsOn(playJsonRpc % "test->test")
