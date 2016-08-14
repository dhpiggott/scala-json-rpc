organization := "com.dhpcs"

name := "play-json-rpc"

version := "1.2.0"

scalaVersion := "2.11.8"

publishArtifact in Test := true

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
