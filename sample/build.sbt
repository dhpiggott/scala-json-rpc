name := "play-json-rpc-sample"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.dhpcs" %% "play-json-rpc" % "1.0.0-SNAPSHOT",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.dhpcs" %% "play-json-rpc" % "1.0.0-SNAPSHOT" % "test->test"
)
