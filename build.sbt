name := """play-json-rpc"""

organization := "com.dhpcs"

version := "0.6.0-SNAPSHOT"

scalaVersion := "2.11.7"

publishArtifact in Test := true

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.9",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)
