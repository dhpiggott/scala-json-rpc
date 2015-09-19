play-json-rpc
=============

A Scala library providing implicit [JSON Formats](https://www.playframework.com/documentation/2.3.9/api/scala/index.html#play.api.libs.json.package) for [JSON-RPC 2.0](http://www.jsonrpc.org/specification) messages, built on top of the Play! Framework's standalone [play-json](https://www.playframework.com/documentation/2.3.x/ScalaJson) library. It does *not* depend on the whole of Play, so the dependency footprint is relatively small and you can use it in a wide range of applications - including in Android projects.


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


Notes on usage in Android projects
----------------------------------

Use in Android projects is very much supported. The motivation behind building the library was in being able to define message types and supporting code only once (both the JSON-RPC formats and formats for domain objects), and then use them both on the server and client side of the downstream project.

However, there are two caveats:

1. play-json-rpc's dependency on play-json and its transitive dependencies will result in your application exceeding the [DEX 65K Methods Limit](https://developer.android.com/tools/building/multidex.html). You will need to workaround this. The recommended approach is to use MultiDex as described at https://github.com/saturday06/gradle-android-scala-plugin#52-option-2-use-multidex. Note that unless you are writing Scala code in the Android application itself you do *not* need to add `compile "org.scala-lang:scala-library:2.11.7"` to your dependencies, just as you do not need to use the gradle-android-scala plugin.
2. In Play 2.4, ["the support for Java 6 and Java 7 was dropped and Play 2.4 now requires Java 8."](https://playframework.com/documentation/2.4.x/Migration24). This means that the 2.4.0 standalone play-json library is also compiled with JDK8. In order to support use in Android projects play-json-rpc depends on play-json 2.3.9 - the last release compiled with JDK7. As long as Android does not support Java 8 features, changing the dependency to 2.4.0 will result in build time errors in Android projects.

 Fixing the dependency at 2.3.9 introduces its own problems in that Play! 2.4.x+ projects will override the play-json dependecy to 2.4.x, which can result in runtime errors if the runtime play-json version e.g. does not have methods that were present in 2.3.9 which play-json-rpc is compiled against.

 Specifically, between play-json 2.3.9 and play-json 2.4.0 there was a change in how JsObjects are created. In 2.3.9 the [case class constructor accepted a fields sequence](https://github.com/playframework/playframework/blob/2.3.9/framework/src/play-json/src/main/scala/play/api/libs/json/JsValue.scala#L166), while in 2.4.0 the case class constructor has changed and to create JsObjects from a sequence of fields entails calling a [new apply method on the companion object](https://github.com/playframework/playframework/blob/2.4.0/framework/src/play-json/src/main/scala/play/api/libs/json/JsValue.scala#L154). As such, attempting to create a JsObject with a play-json runtime version of 2.4.0 but with code that was compiled against 2.3.9 results in `NoSuchMethodErrors`.

 play-json-rpc accomodates this difference by instead using the `Json.obj()` JsObject creation function, which is present in both 2.3.9 and 2.4.0.


Contributing
------------


Contributations - both code and documentation - are welcome.

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
