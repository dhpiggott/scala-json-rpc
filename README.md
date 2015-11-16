play-json-rpc
=============

A Scala library providing implicit [play-json Formats](https://www.playframework.com/documentation/2.3.x/ScalaJson) for [JSON-RPC 2.0](http://www.jsonrpc.org/specification) messages, built on top of the Play! Framework's standalone `play-json` library. It does *not* depend on the whole of Play, so the dependency footprint is relatively small and you can use it in a wide range of applications – including in Android projects.

SBT dependency
--------------

Just add the following lines to your `build.sbt`:

```scala
resolvers += "dhpcs at bintray" at "https://dl.bintray.com/dhpcs/maven"

libraryDependencies += "com.dhpcs" %% "play-json-rpc" % "1.0.0"
```

Gradle dependency
-----------------

For (e.g.) Android Gradle projects, first add to the `repositories` block of your top-level `build.gradle`:

```groovy
allprojects {
    repositories {
        jcenter()
        maven {
            url  "https://dl.bintray.com/dhpcs/maven"
        }
    }
}
```

Then add to the `dependencies` block of your main module's `build.gradle`:

```groovy
dependencies {
    compile 'com.dhpcs:play-json-rpc_2.11:1.0.0'
}
```

Structure
---------

`play-json-rpc` essentially provides two things:

1. Types and formats for JSON-RPC 2.0 messages. The types are as follows:

   1. `JsonRpcRequestMessage`:

      ```scala
      case class JsonRpcRequestMessage(method: String,
                                       params: Either[JsArray, JsObject],
                                       id: Option[Either[String, BigDecimal]]) extends JsonRpcMessage
      ```

   2. `JsonRpcRequestMessageBatch`:

      ```scala
      case class JsonRpcRequestMessageBatch(messages: Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]])
        extends JsonRpcMessage {
        require(messages.nonEmpty)
      }
      ```

   3. `JsonRpcResponseMessage`:

      ```scala
      case class JsonRpcResponseMessage(eitherErrorOrResult: Either[JsonRpcResponseError, JsValue],
                                        id: Option[Either[String, BigDecimal]]) extends JsonRpcMessage
      ```

   4. `JsonRpcResponseMessageBatch`:

      ```scala
      case class JsonRpcResponseMessageBatch(messages: Seq[JsonRpcResponseMessage]) extends JsonRpcMessage {
        require(messages.nonEmpty)
      }
      ```

   5. `JsonRpcNotificationMessage`:

      ```scala
      case class JsonRpcNotificationMessage(method: String,
                                            params: Either[JsArray, JsObject]) extends JsonRpcMessage
      ```

 The companion object for the `JsonRpcMessage` trait has an implicitly available `JsonRpcMessageFormat` that can read and write all of the above types – you can then match on the type of the result.

2. A set of abstract classes as bases for companion objects providing read/write functions for a hierarchy of application level types (see example usage).

Example usage
-------------

This is best demonstrated by the sample project (located in the `sample` directory).

The best way to get started with `play-json-rpc` in your project is to copy the sample project and then add/change/remove domain and message types as necessary. You might want to copy and modify the sample tests too.

The sample project includes:

1. Two domain types with JSON Formats, `Account` and `Transaction` (definitions omitted for brevity here because they are fairly typical `play-json` usage).

2. Two command message types (`UpdateAccountCommand` and `AddTransactionCommand`), two matching response types (`UpdateAccountResponse` and `AddTransactionResponse`) and two notification message types (`AccountUpdatedNotification` and `TransactionAddedNotification`).

   Note that `play-json-rpc` leaves the definition of the `Message` type hierarchy to your code in order that `Message` and all its subtypes can be defined as sealed to let you benefit from the [compiler checks](http://www.scala-lang.org/old/node/123) which sealed enables.

   Thus most users should copy and preserve the definitions of `Message`, `Command`, `Response`, `ResultResponse` and `Notification`, and simply replace the implementations of `Command`, `ResultResponse` and `Notfication` with their own.

3. Partial code for a server that receives messages of type `JsonRpcRequestMessage` and sends messages of type `JsonRpcResponseMessage` and `JsonRpcNotificationMessage`.

4. Partial code for a client that sends messages of type `JsonRpcRequestMessage` and receives messages of type `JsonRpcResponseMessage` and `JsonRpcNotificationMessage`.

### Sample message types

The types that you should define as a user of `play-json-rpc` will look something like the following.

The full definition with imports is at [sample/src/main/scala/com/dhpcs/jsonrpc/sample/models/Message.scala](sample/src/main/scala/com/dhpcs/jsonrpc/sample/models/Message.scala).

```scala
sealed trait Message

sealed trait Command extends Message

case class UpdateAccountCommand(account: Account) extends Command

case class AddTransactionCommand(from: Int,
                                 to: Int,
                                 value: BigDecimal,
                                 description: Option[String] = None,
                                 metadata: Option[JsObject] = None) extends Command {
  require(value >= 0)
}

object AddTransactionCommand {

  implicit val AddTransactionCommandFormat: Format[AddTransactionCommand] = (
    (JsPath \ "from").format[Int] and
      (JsPath \ "to").format[Int] and
      (JsPath \ "value").format(min[BigDecimal](0)) and
      (JsPath \ "description").formatNullable[String] and
      (JsPath \ "metadata").formatNullable[JsObject]
    )((from, to, value, description, metadata) =>
    AddTransactionCommand(
      from,
      to,
      value,
      description,
      metadata
    ), addTransactionCommand =>
    (addTransactionCommand.from,
      addTransactionCommand.to,
      addTransactionCommand.value,
      addTransactionCommand.description,
      addTransactionCommand.metadata)
    )

}

object Command extends CommandCompanion[Command] {

  override val CommandTypeFormats = MethodFormats(
    "updateAccount" -> Json.format[UpdateAccountCommand],
    "addTransaction" -> Json.format[AddTransactionCommand]
  )

}

sealed trait Response extends Message

sealed trait ResultResponse extends Response

case object UpdateAccountResponse extends ResultResponse

case class AddTransactionResponse(created: Long) extends ResultResponse

object Response extends ResponseCompanion[ResultResponse] {

  override val ResponseFormats = MethodFormats(
    "updateAccount" -> UpdateAccountResponse,
    "addTransaction" -> Json.format[AddTransactionResponse]
  )

}

sealed trait Notification extends Message

case class AccountUpdatedNotification(account: Account) extends Notification

case class TransactionAddedNotification(transaction: Transaction) extends Notification

object Notification extends NotificationCompanion[Notification] {

  override val NotificationFormats = MethodFormats(
    "accountUpdated" -> Json.format[AccountUpdatedNotification],
    "transactionAdded" -> Json.format[TransactionAddedNotification]
  )

}
```

### Sample server code

Note that `readCommand` is not included in `play-json-rpc` itself because different use cases may have different directionality. The following sample receives commands and sends responses and notifications; other use cases might receive commands and notifications but only send responses.

The full definition with imports is at [sample/src/main/scala/com/dhpcs/jsonrpc/sample/Server.scala](sample/src/main/scala/com/dhpcs/jsonrpc/sample/Server.scala).

```scala
object Server {

  private def readCommand(jsonString: String):
  (Option[Either[String, BigDecimal]], Either[JsonRpcResponseError, Command]) =

    Try(Json.parse(jsonString)) match {

      case Failure(exception) =>

        None -> Left(
          JsonRpcResponseError.parseError(exception)
        )

      case Success(json) =>

        Json.fromJson[JsonRpcRequestMessage](json).fold(

          errors => None -> Left(
            JsonRpcResponseError.invalidRequest(errors)
          ),

          jsonRpcRequestMessage =>

            Command.read(jsonRpcRequestMessage)
              .fold[(Option[Either[String, BigDecimal]], Either[JsonRpcResponseError, Command])](

                jsonRpcRequestMessage.id -> Left(
                  JsonRpcResponseError.methodNotFound(jsonRpcRequestMessage.method)
                )

              )(commandJsResult => commandJsResult.fold(

              errors => jsonRpcRequestMessage.id -> Left(
                JsonRpcResponseError.invalidParams(errors)
              ),

              command => jsonRpcRequestMessage.id -> Right(
                command
              )

            ))

        )

    }

}

class Server {

  private def deliverToSender(jsonString: String) = ???

  private def deliverToSubscribers(jsonString: String) = ???

  def yourMessageHandler(jsonString: String) {

    readCommand(jsonString) match {

      case (id, Left(jsonRpcResponseError)) =>

        val jsonResponseString = Json.stringify(Json.toJson(
          JsonRpcResponseMessage(Left(jsonRpcResponseError), id)
        ))

        deliverToSender(jsonResponseString)

      case (id, Right(command)) =>

        command match {

          case UpdateAccountCommand(account) =>

            // Omitted: validate and handle the command
            // ...
            // For demonstration purposes we'll toss a coin to decide whether to reject the command
            if (Random.nextBoolean()) {

              // Rejecting a command:

              val jsonResponseString = Json.stringify(Json.toJson(
                Response.write(
                  Left(
                    ErrorResponse(
                      JsonRpcResponseError.ReservedErrorCodeFloor - 1,
                      "Fate does not favour you today"
                    )
                  ),
                  id
                )
              ))

              deliverToSender(jsonResponseString)

            } else {

              val jsonResponseString = Json.stringify(Json.toJson(
                Response.write(
                  Right(
                    UpdateAccountResponse
                  ),
                  id
                )
              ))

              deliverToSender(jsonResponseString)

              val jsonNotificationString = Json.stringify(Json.toJson(
                Notification.write(
                  AccountUpdatedNotification(
                    account
                  )
                )
              ))

              deliverToSubscribers(jsonNotificationString)

            }

          case AddTransactionCommand(from, to, value, description, metadata) =>

            // Omitted: validate and handle the command
            // ...
            // For demonstration purposes we'll toss a coin to decide whether to reject the command
            if (Random.nextBoolean()) {

              // Rejecting a command:

              val jsonResponseString = Json.stringify(Json.toJson(
                Response.write(
                  Left(
                    ErrorResponse(
                      JsonRpcResponseError.ReservedErrorCodeFloor - 1,
                      "Fate does not favour you today"
                    )
                  ),
                  id
                )
              ))

              deliverToSender(jsonResponseString)

            } else {

              val transaction = Transaction(from, to, value, System.currentTimeMillis, description, metadata)

              val jsonResponseString = Json.stringify(Json.toJson(
                Response.write(
                  Right(
                    AddTransactionResponse(
                      transaction.created
                    )
                  ),
                  id
                )
              ))

              deliverToSender(jsonResponseString)

              val jsonNotificationString = Json.stringify(Json.toJson(
                Notification.write(
                  TransactionAddedNotification(
                    transaction
                  )
                )
              ))

              deliverToSubscribers(jsonNotificationString)

            }

        }

    }

  }

}
```

### Sample client code

Note that `readJsonRpcMessage` is not included in `play-json-rpc` itself, for the same reason that the server sample's `readCommand` is not.

Note also that in the real project this sample is based on, there is extra code that ensures `pendingRequests` etc. is only accessed by one thread. That code was removed to simplify this sample.

The full definition with imports is at [sample/src/main/scala/com/dhpcs/jsonrpc/sample/Client.scala](sample/src/main/scala/com/dhpcs/jsonrpc/sample/Client.scala).

```scala
object Client {

  trait ResponseCallback {

    def onErrorReceived(errorResponse: ErrorResponse)

    def onResultReceived(resultResponse: ResultResponse) = ()

  }

  private case class PendingRequest(requestMessage: JsonRpcRequestMessage,
                                    callback: ResponseCallback)

  private def readJsonRpcMessage(jsonString: String): Either[String, JsonRpcMessage] =

    Try(Json.parse(jsonString)) match {

      case Failure(exception) =>

        Left(s"Invalid JSON: $exception")

      case Success(json) =>

        Json.fromJson[JsonRpcMessage](json).fold({ errors =>
          Left(s"Invalid JSON-RPC message: $errors")
        }, Right(_))

    }

}

class Client {

  private var pendingRequests = Map.empty[BigDecimal, PendingRequest]
  private var commandIdentifier = BigDecimal(0)

  private def deliverToServer(jsonString: String) = ???

  private def disconnect(reason: Int) = ???

  private def notifySubscribers(notification: Notification) = ???

  def sendCommand(command: Command, responseCallback: ResponseCallback) {
    val jsonRpcRequestMessage = Command.write(command, Some(Right(commandIdentifier)))
    commandIdentifier = commandIdentifier + 1
    val jsonCommandString = Json.stringify(
      Json.toJson(jsonRpcRequestMessage)
    )
    deliverToServer(jsonCommandString)
    pendingRequests = pendingRequests +
      (jsonRpcRequestMessage.id.get.right.get ->
        PendingRequest(jsonRpcRequestMessage, responseCallback))
  }

  def yourMessageHandler(jsonString: String) {

    readJsonRpcMessage(jsonString) match {

      case Left(error) =>

        disconnect(1002)
        sys.error(error)

      case Right(jsonRpcMessage) => jsonRpcMessage match {

        case jsonRpcRequestMessage: JsonRpcRequestMessage =>

          disconnect(1002)
          sys.error(s"Received $jsonRpcRequestMessage")

        case jsonRpcRequestMessageBatch: JsonRpcRequestMessageBatch =>

          disconnect(1002)
          sys.error(s"Received $jsonRpcRequestMessageBatch")

        case jsonRpcResponseMessage: JsonRpcResponseMessage =>

          jsonRpcResponseMessage.id.fold {
            disconnect(1002)
            sys.error(s"JSON-RPC message ID missing, jsonRpcResponseMessage" +
              s".eitherErrorOrResult=${jsonRpcResponseMessage.eitherErrorOrResult}")
          } { id =>
            id.right.toOption.fold {
              sys.error(s"JSON-RPC message ID was not a number, id=$id")
              disconnect(1002)
            } { commandIdentifier =>
              pendingRequests.get(commandIdentifier).fold {
                disconnect(1002)
                sys.error(s"No pending request exists with commandIdentifier" +
                  s"=$commandIdentifier")
              } { pendingRequest =>
                pendingRequests = pendingRequests - commandIdentifier
                Response.read(
                  jsonRpcResponseMessage,
                  pendingRequest.requestMessage.method
                ).fold({ errors =>
                  disconnect(1002)
                  sys.error(s"Invalid Response: $errors")
                }, {

                  case Left(errorResponse) =>

                    pendingRequest.callback.onErrorReceived(errorResponse)

                  case Right(resultResponse) =>

                    pendingRequest.callback.onResultReceived(resultResponse)

                })
              }
            }
          }

        case jsonRpcResponseMessageBatch: JsonRpcResponseMessageBatch =>

          disconnect(1002)
          sys.error(s"Received $jsonRpcResponseMessageBatch")

        case jsonRpcNotificationMessage: JsonRpcNotificationMessage =>

          Notification.read(jsonRpcNotificationMessage).fold {
            disconnect(1002)
            sys.error(s"No notification type exists with method" +
              s"=${jsonRpcNotificationMessage.method}")
          }(_.fold({ errors =>
            disconnect(1002)
            sys.error(s"Invalid Notification: $errors")
          }, notifySubscribers
          ))

      }

    }

  }

}
```

Error responses
---------------

A `JsonRpcResponseError` looks like this:

```scala
sealed trait JsonRpcResponseError {

  def code: Int

  def message: String

  def data: Option[JsValue]

}
```

And has the following functions available on its companion object with which to create instances:

```scala
def parseError(exception: Throwable): JsonRpcResponseError

def invalidRequest(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError

def methodNotFound(method: String): JsonRpcResponseError

def invalidParams(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError

def internalError(error: Option[JsValue] = None): JsonRpcResponseError

def serverError(code: Int, error: Option[JsValue] = None): JsonRpcResponseError

def applicationError(code: Int,
                     message: String,
                     data: Option[JsValue] = None): JsonRpcResponseError
```

Note that `applicationError(...)` and `serverError(...)` validate the integer code passed in and will fail if it is not legal for the error type as per the JSON-RPC 2.0 spec.

Notes on usage in Android projects
----------------------------------

Use in Android projects is very much supported. The motivation behind building the library was in being able to define message types and supporting code only once (both the JSON-RPC formats and formats for domain objects), and then use them both on the server and client side of the downstream project.

However, there are two caveats:

1. The `play-json-rpc` dependency on `play-json` and its transitive dependencies will result in your application exceeding the [DEX 65K Methods Limit](https://developer.android.com/tools/building/multidex.html). You will need to workaround this. The recommended approach is to use MultiDex as described at https://github.com/saturday06/gradle-android-scala-plugin#52-option-2-use-multidex. Note that unless you are writing Scala code in the Android application itself you do *not* need to add `compile "org.scala-lang:scala-library:2.11.7"` to your dependencies, just as you do not need to use the gradle-android-scala plugin.

2. In Play 2.4, ["the support for Java 6 and Java 7 was dropped and Play 2.4 now requires Java 8."](https://playframework.com/documentation/2.4.x/Migration24). This means that the 2.4.0 standalone play-json library is also compiled with JDK8. In order to support use in Android projects `play-json-rpc` depends on `play-json` 2.3.9 – the last release compiled with JDK7. As long as Android does not support Java 8 features, changing the dependency to 2.4.0 will result in build time errors in Android projects.

   Fixing the dependency at 2.3.9 introduces its own problems in that Play! 2.4.x+ projects will override the `play-json` dependency to 2.4.x, which can result in runtime errors if the runtime play-json version e.g. does not have methods that were present in 2.3.9 which `play-json-rpc` is compiled against.

   Specifically, between `play-json` 2.3.9 and `play-json` 2.4.0 there was a change in how JsObjects are created. In 2.3.9 the [case class constructor accepted a fields sequence](https://github.com/playframework/playframework/blob/2.3.9/framework/src/play-json/src/main/scala/play/api/libs/json/JsValue.scala#L166), while in 2.4.0 the case class constructor has changed and to create JsObjects from a sequence of fields entails calling a [new apply method on the companion object](https://github.com/playframework/playframework/blob/2.4.0/framework/src/play-json/src/main/scala/play/api/libs/json/JsValue.scala#L154). As such, attempting to create a JsObject with a `play-json` runtime version of 2.4.0 but with code that was compiled against 2.3.9 results in `NoSuchMethodErrors`.

   `play-json-rpc` accommodates this difference by instead using the `Json.obj()` JsObject creation function, which is present in both 2.3.9 and 2.4.0.

Notes on using the test artifact in SBT projects
------------------------------------------------

There are two types in the `com.dhpcs.json` package that may be useful when writing tests for dependent projects (example use can be seen in `JsonRpcMessageSpec.scala`):
1. `JsResultUniformity`
2. `FormatBehaviors`

To enable use of these types in test code of dependent projects, the `play-json-rpc` test classes are published alongside the main classes, in their own `.jar`.

To make use of these classes in your dependent project's tests, you would set your dependencies to be like this:

```scala
libraryDependencies ++= Seq(
  "com.dhpcs" %% "play-json-rpc" % "1.0.0",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "com.dhpcs" %% "play-json-rpc" % "1.0.0" % "test->test"
)
```

Unfortunately, due to https://github.com/sbt/sbt/issues/1827, if you try to do this using the Maven artifacts on Bintray, you will get a build error with words to the effect of "the test configuration is not public".

Until the SBT issue is fixed you have two options to workaround this:

* Remove the Bintray repository from your `build.sbt`, clone `play-json-rpc` and deploy the version you want to use directly to your local Ivy cache by running `./activator publishLocal`.

 With this the disadvantage  is that you have to clone and deploy `play-json-rpc` yourself.

or

* Continue using the Bintray artifacts but change the SBT dependency line to look like this:
  ```scala
   "com.dhpcs" %% "play-json-rpc" % "1.0.0" % "test" classifier("tests")
  ```

 With this the disadvantage is that the main configuration of the POM produced by your dependent project will unnecessarily depend on the test configuration `.jar` of `play-json-rpc`.

Contributing
------------

Contributions – both code and documentation – are welcome.

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
