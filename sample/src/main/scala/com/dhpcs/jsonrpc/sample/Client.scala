package com.dhpcs.jsonrpc.sample

import com.dhpcs.jsonrpc._
import com.dhpcs.jsonrpc.sample.Client._
import com.dhpcs.jsonrpc.sample.models._
import play.api.libs.json.Json

import scala.util.{Failure, Right, Success, Try}

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
