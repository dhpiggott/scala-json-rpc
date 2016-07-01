package com.dhpcs.jsonrpc

import com.dhpcs.jsonrpc.Message.MethodFormat
import com.dhpcs.jsonrpc.ResponseCompanion.ErrorResponse
import play.api.libs.json._

import scala.reflect.ClassTag

trait CommandCompanion[A] {
  protected[this] val CommandTypeFormats: Seq[MethodFormat[_ <: A]]

  def read(jsonRpcRequestMessage: JsonRpcRequestMessage): Option[JsResult[A]] =
    CommandTypeFormats.find(_.methodName == jsonRpcRequestMessage.method).map(
      typeChoiceMapping => jsonRpcRequestMessage.params.fold(
        _ => JsError("command parameters must be named"),
        jsObject => typeChoiceMapping.fromJson(jsObject)
      )
    ).map(_.fold(
      // We do this in order to drop any non-root path that may have existed in the success case.
      invalid => JsError(invalid),
      valid => JsSuccess(valid)
    ))

  def write(command: A, id: Option[Either[String, BigDecimal]]): JsonRpcRequestMessage = {
    val mapping = CommandTypeFormats.find(_.matchesInstance(command))
      .getOrElse(sys.error(s"No format found for ${command.getClass}"))
    JsonRpcRequestMessage(mapping.methodName, Right(mapping.toJson(command).asInstanceOf[JsObject]), id)
  }
}

trait ResponseCompanion[A] {
  protected[this] val ResponseFormats: Seq[MethodFormat[_ <: A]]

  def read(jsonRpcResponseMessage: JsonRpcResponseMessage, method: String): JsResult[Either[ErrorResponse, A]] =
    jsonRpcResponseMessage.eitherErrorOrResult.fold(
      error => JsSuccess(ErrorResponse(error.code, error.message, error.data)).map(Left(_)),
      result => ResponseFormats.find(_.methodName == method).get.fromJson(result).map(Right(_))
    ).fold(
      // We do this in order to drop any non-root path that may have existed in the success case.
      invalid => JsError(invalid),
      valid => JsSuccess(valid)
    )

  def write(response: Either[ErrorResponse, A], id: Option[Either[String, BigDecimal]]): JsonRpcResponseMessage = {
    val eitherErrorOrResult = response match {
      case Left(ErrorResponse(code, message, data)) => Left(
        JsonRpcResponseError.applicationError(code, message, data)
      )
      case Right(resultResponse) =>
        val mapping = ResponseFormats.find(_.matchesInstance(resultResponse))
          .getOrElse(sys.error(s"No format found for ${response.getClass}"))
        Right(mapping.toJson(resultResponse))
    }
    JsonRpcResponseMessage(eitherErrorOrResult, id)
  }
}

object ResponseCompanion {

  case class ErrorResponse(code: Int,
                           message: String,
                           data: Option[JsValue] = None)

}

trait NotificationCompanion[A] {
  protected[this] val NotificationFormats: Seq[MethodFormat[_ <: A]]

  def read(jsonRpcNotificationMessage: JsonRpcNotificationMessage): Option[JsResult[A]] =
    NotificationFormats.find(_.methodName == jsonRpcNotificationMessage.method).map(
      typeChoiceMapping => jsonRpcNotificationMessage.params.fold(
        _ => JsError("notification parameters must be named"),
        jsObject => typeChoiceMapping.fromJson(jsObject)
      )
    ).map(_.fold(
      // We do this in order to drop any non-root path that may have existed in the success case.
      invalid => JsError(invalid),
      valid => JsSuccess(valid)
    ))

  def write(notification: A): JsonRpcNotificationMessage = {
    val mapping = NotificationFormats.find(_.matchesInstance(notification))
      .getOrElse(sys.error(s"No format found for ${notification.getClass}"))
    JsonRpcNotificationMessage(mapping.methodName, Right(mapping.toJson(notification).asInstanceOf[JsObject]))
  }
}

object Message {

  abstract class MethodFormat[A](methodAndFormatOrObject: (String, Either[A, Format[A]]))
                                (implicit val classTag: ClassTag[A]) {
    val (methodName, formatOrObject) = methodAndFormatOrObject

    def fromJson(json: JsValue): JsResult[A] = formatOrObject.fold(
      commandResponse => JsSuccess(commandResponse),
      format => format.reads(json)
    )

    def matchesInstance(o: Any): Boolean = classTag.runtimeClass.isInstance(o)

    def toJson(o: Any): JsValue = formatOrObject.fold(
      _ => Json.obj(),
      format => format.writes(o.asInstanceOf[A])
    )
  }

  implicit class MethodFormatFormat[A](methodAndFormat: (String, Format[A]))(implicit classTag: ClassTag[A])
    extends MethodFormat(methodAndFormat._1, Right(methodAndFormat._2))(classTag)

  implicit class MethodFormatObject[A](methodAndObject: (String, A))(implicit classTag: ClassTag[A])
    extends MethodFormat(methodAndObject._1, Left(methodAndObject._2))(classTag)

  object MethodFormats {
    def apply[A](methodFormats: MethodFormat[_ <: A]*): Seq[MethodFormat[_ <: A]] = {
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
