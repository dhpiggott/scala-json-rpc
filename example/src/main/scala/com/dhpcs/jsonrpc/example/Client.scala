package com.dhpcs.jsonrpc.example

import com.dhpcs.jsonrpc._
import com.dhpcs.jsonrpc.example.Client._
import com.dhpcs.jsonrpc.example.models._
import com.dhpcs.jsonrpc.ResponseCompanion.ErrorResponse
import play.api.libs.json.Json

import scala.util.{Failure, Right, Success, Try}

object Client {

  trait ResponseCallback {
    def onErrorReceived(errorResponse: ErrorResponse): Unit
    def onResultReceived(resultResponse: ResultResponse): Unit = ()
  }

  private case class PendingRequest(requestMessage: JsonRpcRequestMessage, callback: ResponseCallback)

  private def readJsonRpcMessage(jsonString: String): Either[String, JsonRpcMessage] =
    Try(Json.parse(jsonString)) match {
      case Failure(exception) =>
        Left(s"Invalid JSON: $exception")
      case Success(json) =>
        Json
          .fromJson[JsonRpcMessage](json)
          .fold(
            errors => Left(s"Invalid JSON-RPC message: $errors"),
            jsonRpcMessage => Right(jsonRpcMessage)
          )
    }
}

class Client {

  private[this] var pendingRequests   = Map.empty[BigDecimal, PendingRequest]
  private[this] var commandIdentifier = BigDecimal(0)

  def sendCommand(command: Command, responseCallback: ResponseCallback): Unit = {
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

  def yourMessageHandler(jsonString: String): Unit = {
    readJsonRpcMessage(jsonString) match {
      case Left(error) =>
        sys.error(error)
      case Right(jsonRpcMessage) =>
        jsonRpcMessage match {
          case jsonRpcRequestMessage: JsonRpcRequestMessage =>
            sys.error(s"Received $jsonRpcRequestMessage")
          case jsonRpcRequestMessageBatch: JsonRpcRequestMessageBatch =>
            sys.error(s"Received $jsonRpcRequestMessageBatch")
          case jsonRpcResponseMessage: JsonRpcResponseMessage =>
            jsonRpcResponseMessage.id.fold {
              sys.error(
                s"JSON-RPC message ID missing, jsonRpcResponseMessage" +
                  s".errorOrResult=${jsonRpcResponseMessage.errorOrResult}")
            } { id =>
              id.right.toOption.fold {
                sys.error(s"JSON-RPC message ID was not a number, id=$id")
              } { commandIdentifier =>
                pendingRequests
                  .get(commandIdentifier)
                  .fold {
                    sys.error(s"No pending request exists with commandIdentifier" +
                      s"=$commandIdentifier")
                  } { pendingRequest =>
                    pendingRequests = pendingRequests - commandIdentifier
                    Response
                      .read(
                        jsonRpcResponseMessage,
                        pendingRequest.requestMessage.method
                      )
                      .fold(
                        { errors =>
                          sys.error(s"Invalid Response: $errors")
                        }, {
                          case Left(errorResponse) =>
                            pendingRequest.callback.onErrorReceived(errorResponse)
                          case Right(resultResponse) =>
                            pendingRequest.callback.onResultReceived(resultResponse)
                        }
                      )
                  }
              }
            }
          case jsonRpcResponseMessageBatch: JsonRpcResponseMessageBatch =>
            sys.error(s"Received $jsonRpcResponseMessageBatch")
          case jsonRpcNotificationMessage: JsonRpcNotificationMessage =>
            Notification
              .read(jsonRpcNotificationMessage)
              .fold {
                sys.error(
                  s"No notification type exists with method" +
                    s"=${jsonRpcNotificationMessage.method}")
              }(_.fold({ errors =>
                sys.error(s"Invalid Notification: $errors")
              }, notifySubscribers))
        }
    }
  }

  private[this] def deliverToServer(jsonString: String): Unit           = ()
  private[this] def notifySubscribers(notification: Notification): Unit = ()

}
