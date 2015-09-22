package com.dhpcs.jsonrpc.sample

import com.dhpcs.jsonrpc._
import com.dhpcs.jsonrpc.sample.Message.MethodFormats
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.reflect.ClassTag

sealed trait Message

object Message {

  abstract class MethodFormat[A](methodAndFormatOrObject: (String, Either[A, Format[A]]))
                                (implicit val classTag: ClassTag[A]) {

    val (methodName, formatOrObject) = methodAndFormatOrObject

    def fromJson(jsValue: JsValue) = formatOrObject.fold(
      commandResponse => JsSuccess(commandResponse),
      format => format.reads(jsValue)
    )

    def matchesInstance(o: Any) = classTag.runtimeClass.isInstance(o)

    def toJson(o: Any) = formatOrObject.fold(
      _ => Json.obj(),
      format => format.writes(o.asInstanceOf[A])
    )

  }

  implicit class MethodFormatFormat[A](methodAndFormat: (String, Format[A]))(implicit classTag: ClassTag[A])
    extends MethodFormat(methodAndFormat._1, Right(methodAndFormat._2))(classTag)

  implicit class MethodFormatObject[A](methodAndObject: (String, A))(implicit classTag: ClassTag[A])
    extends MethodFormat(methodAndObject._1, Left(methodAndObject._2))(classTag)

  object MethodFormats {

    def apply[A](methodFormats: MethodFormat[_ <: A]*) = {
      val methodNames = methodFormats.map(_.methodName)
      require(
        methodNames == methodNames.distinct,
        "Duplicate method names: " + methodNames.mkString(", ")
      )
      val overlappingTypes = methodFormats.combinations(2).filter {
        case Seq(first, second) => first.classTag.runtimeClass isAssignableFrom second.classTag.runtimeClass
      }
      require(
        overlappingTypes.isEmpty,
        "Overlapping types: " + overlappingTypes.map {
          case Seq(first, second) => s"${first.classTag} is assignable from ${second.classTag}"
        }.mkString(", ")
      )
      methodFormats
    }

  }

}

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

object Command {

  val CommandTypeFormats = MethodFormats(
    "updateAccount" -> Json.format[UpdateAccountCommand],
    "addTransaction" -> Json.format[AddTransactionCommand]
  )

  def read(jsonRpcRequestMessage: JsonRpcRequestMessage): Option[JsResult[Command]] =
    CommandTypeFormats.find(_.methodName == jsonRpcRequestMessage.method).map(
      typeChoiceMapping => jsonRpcRequestMessage.params.fold(
        _ => JsError("command parameters must be named"),
        jsObject => typeChoiceMapping.fromJson(jsObject)
      )
    ).map(_.fold(

      /*
       * We do this in order to drop any non-root path that may have existed in the success case.
       */
      invalid => JsError(invalid),
      valid => JsSuccess(valid)
    ))

  def write(command: Command, id: Option[Either[String, BigDecimal]]): JsonRpcRequestMessage = {
    val mapping = CommandTypeFormats.find(_.matchesInstance(command))
      .getOrElse(sys.error(s"No format found for ${command.getClass}"))
    JsonRpcRequestMessage(mapping.methodName, Right(mapping.toJson(command).asInstanceOf[JsObject]), id)
  }

}

sealed trait Response extends Message

case class ErrorResponse(code: Int,
                         message: String,
                         data: Option[JsValue] = None) extends Response

sealed trait ResultResponse extends Response

case object UpdateAccountResponse extends ResultResponse

case class AddTransactionResponse(created: Long) extends ResultResponse

object Response {

  val ResponseFormats = MethodFormats(
    "updateAccount" -> UpdateAccountResponse,
    "addTransaction" -> Json.format[AddTransactionResponse]
  )

  def read(jsonRpcResponseMessage: JsonRpcResponseMessage, method: String): JsResult[Response] =
    jsonRpcResponseMessage.eitherErrorOrResult.fold(
      error => JsSuccess(ErrorResponse(error.code, error.message, error.data)),
      result => ResponseFormats.find(_.methodName == method).get.fromJson(result)
    ).fold(

        /*
         * We do this in order to drop any non-root path that may have existed in the success case.
         */
        invalid => JsError(invalid),
        valid => JsSuccess(valid)
      )

  def write(response: Response, id: Option[Either[String, BigDecimal]]): JsonRpcResponseMessage = {
    val eitherErrorOrResult = response match {
      case ErrorResponse(code, message, data) => Left(
        JsonRpcResponseError.applicationError(code, message, data)
      )
      case resultResponse: ResultResponse =>
        val mapping = ResponseFormats.find(_.matchesInstance(resultResponse))
          .getOrElse(sys.error(s"No format found for ${response.getClass}"))
        Right(mapping.toJson(response))
    }
    JsonRpcResponseMessage(eitherErrorOrResult, id)
  }

}

sealed trait Notification extends Message

case class AccountUpdatedNotification(account: Account) extends Notification

case class TransactionAddedNotification(transaction: Transaction) extends Notification

object Notification {

  val NotificationFormats = MethodFormats(
    "accountUpdated" -> Json.format[AccountUpdatedNotification],
    "transactionAdded" -> Json.format[TransactionAddedNotification]
  )

  def read(jsonRpcNotificationMessage: JsonRpcNotificationMessage): Option[JsResult[Notification]] =
    NotificationFormats.find(_.methodName == jsonRpcNotificationMessage.method).map(
      typeChoiceMapping => jsonRpcNotificationMessage.params.fold(
        _ => JsError("notification parameters must be named"),
        jsObject => typeChoiceMapping.fromJson(jsObject)
      )
    ).map(_.fold(

      /*
       * We do this in order to drop any non-root path that may have existed in the success case.
       */
      invalid => JsError(invalid),
      valid => JsSuccess(valid)
    ))

  def write(notification: Notification): JsonRpcNotificationMessage = {
    val mapping = NotificationFormats.find(_.matchesInstance(notification))
      .getOrElse(sys.error(s"No format found for ${notification.getClass}"))
    JsonRpcNotificationMessage(mapping.methodName, Right(mapping.toJson(notification).asInstanceOf[JsObject]))
  }

}