name := "play-json-rpc-sample"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.dhpcs" %% "play-json-rpc" % "1.0.0",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.dhpcs" %% "play-json-rpc" % "1.0.0" % "test->test"
)
