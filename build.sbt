import sbt.Keys._

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "com.dhpcs"
)

lazy val noopPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

lazy val playJson = "com.typesafe.play" %% "play-json" % "2.3.10"

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.0"

lazy val playJsonRpcTestkit = project.in(file("testkit"))
  .settings(commonSettings)
  .settings(
    name := "play-json-rpc-testkit",
    libraryDependencies ++= Seq(
      playJson,
      scalaTest
    )
  )

lazy val playJsonRpc = project.in(file("play-json-rpc"))
  .settings(commonSettings)
  .settings(
    name := "play-json-rpc",
    libraryDependencies ++= Seq(
      playJson,
      scalaTest % Test
    )
  )
  .dependsOn(playJsonRpcTestkit % Test)

lazy val playJsonRpcExample = project.in(file("example"))
  .settings(commonSettings)
  .settings(noopPublish)
  .settings(
    name := "play-json-rpc-example"
  )
  .dependsOn(playJsonRpc)
  .dependsOn(playJsonRpcTestkit)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(noopPublish)
  .settings(
    name := "play-json-rpc-root"
  )
  .aggregate(
    playJsonRpcTestkit,
    playJsonRpc,
    playJsonRpcExample
  )
