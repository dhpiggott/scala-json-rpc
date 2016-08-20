import sbt.Keys._

scalaVersion in ThisBuild := "2.11.8"

lazy val commonSettings = Seq(
  organization := "com.dhpcs"
)

lazy val playJsonRpcTestkit = project.in(file("testkit"))
  .settings(commonSettings)
  .settings(Seq(
    name := "play-json-rpc-testkit",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.10",
      "org.scalatest" %% "scalatest" % "3.0.0"
    )
  ))

lazy val playJsonRpc = project.in(file("play-json-rpc"))
  .settings(commonSettings)
  .settings(Seq(
    name := "play-json-rpc",
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.10",
      "org.scalatest" %% "scalatest" % "3.0.0" % Test
    )
  ))
  .dependsOn(playJsonRpcTestkit % Test)

lazy val playJsonRpcExample = project.in(file("example"))
  .settings(commonSettings)
  .settings(Seq(
    name := "play-json-rpc-example"
  ))
  .dependsOn(playJsonRpc)
  .dependsOn(playJsonRpcTestkit)
