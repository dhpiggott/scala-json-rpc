name := "play-json-rpc-sample"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.dhpcs" %% "play-json-rpc" % "1.2.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % Test,
  "com.dhpcs" %% "play-json-rpc" % "1.2.0" % "test->test"
)
