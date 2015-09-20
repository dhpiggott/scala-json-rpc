play-json-rpc
=============

A Scala library providing implicit [play-json Formats](https://www.playframework.com/documentation/2.3.x/ScalaJson) for [JSON-RPC 2.0](http://www.jsonrpc.org/specification) messages, built on top of the Play! Framework's standalone `play-json` library. It does *not* depend on the whole of Play, so the dependency footprint is relatively small and you can use it in a wide range of applications - including in Android projects.


Building with SBT
-----------------

Just add the following lines to your `build.sbt`:

```scala
resolvers += "dhpcs at bintray" at "http://dl.bintray.com/dhpcs/maven"

libraryDependencies += "com.dhpcs" %% "play-json-rpc" % "0.4.0"
```


Building with Gradle
--------------------

For (e.g.) Android Gradle projects, first add to the `repositories` block of your top-level `build.gradle`:

```groovy
allprojects {
    repositories {
        jcenter()
        maven {
            url  "http://dl.bintray.com/dhpcs/maven"
        }
    }
}
```

Then add to the `dependencies` block of your main module's `build.gradle`:

```groovy
dependencies {
    compile 'com.dhpcs:play-json-rpc_2.11:0.4.0'
}
```


Example usage
-------------

TODO


Conformance with the JSON-RPC 2.0 specification
-----------------------------------------------

`play-json-rpc` does not conform 100% with the JSON-RPC 2.0 specification. That said, it is compatible with other implementations that conform to the SHOULD clauses of the specification. The deviations are with batching (not supported at all) and with identifiers (described below).

The [specification](http://www.jsonrpc.org/specification) says this about request identifiers:
> An identifier established by the Client that MUST contain a String, Number, or NULL value if included. If it is not included it is assumed to be a notification. The value SHOULD normally not be Null [1] and Numbers SHOULD NOT contain fractional parts [2]

The `play-json-rpc` type that represents request messages is defined like this:

```scala
case class JsonRpcRequestMessage(method: String,
                                 params: Either[JsArray, JsObject],
                                 id: Either[String, Int])
```

The two deviations with identifiers are thus:

1. That it only supports integer number identifiers (so no fractional parts, which are permitted but discouraged by the specification). Any request with a fractional numeric identifiers will be read as a `JsError`.
1. It cannot handle request messages that contain a null identifier (which are permitted but discouraged by the specification). Any request with a null (as opposed to missing) identifier field will be read as either `JsError` (if reading as a `JsonRpcRequestMessage`), or as a `JsonRpcNotification` if reading as a `JsonRpcMessage`.


Notes on usage in Android projects
----------------------------------

Use in Android projects is very much supported. The motivation behind building the library was in being able to define message types and supporting code only once (both the JSON-RPC formats and formats for domain objects), and then use them both on the server and client side of the downstream project.

However, there are two caveats:

1. The `play-json-rpc` dependency on `play-json` and its transitive dependencies will result in your application exceeding the [DEX 65K Methods Limit](https://developer.android.com/tools/building/multidex.html). You will need to workaround this. The recommended approach is to use MultiDex as described at https://github.com/saturday06/gradle-android-scala-plugin#52-option-2-use-multidex. Note that unless you are writing Scala code in the Android application itself you do *not* need to add `compile "org.scala-lang:scala-library:2.11.7"` to your dependencies, just as you do not need to use the gradle-android-scala plugin.

1. In Play 2.4, ["the support for Java 6 and Java 7 was dropped and Play 2.4 now requires Java 8."](https://playframework.com/documentation/2.4.x/Migration24). This means that the 2.4.0 standalone play-json library is also compiled with JDK8. In order to support use in Android projects `play-json-rpc` depends on `play-json` 2.3.9 - the last release compiled with JDK7. As long as Android does not support Java 8 features, changing the dependency to 2.4.0 will result in build time errors in Android projects.

 Fixing the dependency at 2.3.9 introduces its own problems in that Play! 2.4.x+ projects will override the `play-json` dependency to 2.4.x, which can result in runtime errors if the runtime play-json version e.g. does not have methods that were present in 2.3.9 which `play-json-rpc` is compiled against.

 Specifically, between `play-json` 2.3.9 and `play-json` 2.4.0 there was a change in how JsObjects are created. In 2.3.9 the [case class constructor accepted a fields sequence](https://github.com/playframework/playframework/blob/2.3.9/framework/src/play-json/src/main/scala/play/api/libs/json/JsValue.scala#L166), while in 2.4.0 the case class constructor has changed and to create JsObjects from a sequence of fields entails calling a [new apply method on the companion object](https://github.com/playframework/playframework/blob/2.4.0/framework/src/play-json/src/main/scala/play/api/libs/json/JsValue.scala#L154). As such, attempting to create a JsObject with a `play-json` runtime version of 2.4.0 but with code that was compiled against 2.3.9 results in `NoSuchMethodErrors`.

 `play-json-rpc` accommodates this difference by instead using the `Json.obj()` JsObject creation function, which is present in both 2.3.9 and 2.4.0.


Notes on using the test artifact in SBT projects
------------------------------------------------

There are two types in the `com.dhpcs.json` package that may be useful when writing tests for dependent projects (example use can be seen in `JsonRpcMessageSpec.scala`):
1. `JsResultUniformity`
1. `FormatBehaviors`


To enable use of these types in test code of dependent projects, the `play-json-rpc` test classes are published alongside the main classes, in their own `.jar`.

To make use of these classes in your dependent project's tests, you would set your dependencies to be like this:

```scala
libraryDependencies ++= Seq(
  "com.dhpcs" %% "play-json-rpc" % "0.4.0",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.dhpcs" %% "play-json-rpc" % "0.4.0" % "test->test"
)
```

Unfortunately, due to https://github.com/sbt/sbt/issues/1827, if you try to do this using the Maven artifacts on Bintray, you will get a build error with words to the effect of "the test configuration is not public".

Until the SBT issue is fixed you have two options to workaround this:
* Remove the Bintray repository from your `build.sbt`, clone `play-json-rpc` and deploy the version you want to use directly to your local Ivy cache by running `./activator publishLocal`.

 With this the disadvantage  is that you have to clone and deploy `play-json-rpc` yourself.

or

* Continue using the Bintray artifacts but change the SBT dependency line to look like this:
```scala
 "com.dhpcs" %% "play-json-rpc" % "0.4.0" % "test" classifier("tests")
```

 With this the disadvantage is that the main configuration of the POM produced by your dependent project will unnecessarily depend on the test configuration `.jar` of `play-json-rpc`.


Contributing
------------

Contributions - both code and documentation - are welcome.

The tests are in `JsonRpcMessageSpec.scala` and can be run with `./activator test`.


License
-------

```
Copyright 2015 David Piggott (https://www.dhpcs.com).

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
