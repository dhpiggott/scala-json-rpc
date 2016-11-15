lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "com.dhpcs",
  scalacOptions in Compile ++= Seq(
    "-target:jvm-1.8",
    "-encoding",
    "UTF-8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xfuture",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-inaccessible",
    "-Ywarn-infer-any",
    "-Ywarn-nullary-override",
    "-Ywarn-nullary-unit",
    "-Ywarn-numeric-widen",
    "-Ywarn-unused",
    "-Ywarn-unused-import"
  )
)

lazy val noopPublish = Seq(
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

lazy val playJson = "com.typesafe.play" %% "play-json" % "2.3.10"

lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"

lazy val playJsonRpcTestkit = project.in(file("testkit"))
  .settings(commonSettings)
  .settings(
    name := "play-json-rpc-testkit"
  )
  .settings(libraryDependencies ++= Seq(
    playJson,
    scalaTest
  ))

lazy val playJsonRpc = project.in(file("play-json-rpc"))
  .settings(commonSettings)
  .settings(
    name := "play-json-rpc"
  )
  .settings(libraryDependencies ++= Seq(
    playJson
  ))
  .dependsOn(playJsonRpcTestkit % Test)
  .settings(libraryDependencies ++= Seq(
    scalaTest % Test
  ))

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
