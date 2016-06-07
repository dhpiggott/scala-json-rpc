package com.dhpcs.jsonrpc.sample

import com.dhpcs.jsonrpc.sample.Server._
import com.dhpcs.jsonrpc.sample.models._
import com.dhpcs.jsonrpc.{ErrorResponse, JsonRpcRequestMessage, JsonRpcResponseError, JsonRpcResponseMessage}
import play.api.libs.json.Json

import scala.util.{Failure, Random, Success, Try}

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
  private[this] def deliverToSender(jsonString: String) = ???

  private[this] def deliverToSubscribers(jsonString: String) = ???

  def yourMessageHandler(jsonString: String) = readCommand(jsonString) match {
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
