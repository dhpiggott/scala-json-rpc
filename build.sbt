organization := "com.dhpcs"

name := "play-json-rpc"

version := "1.1.0"

scalaVersion := "2.11.8"

publishArtifact in Test := true

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.10",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test"
)
