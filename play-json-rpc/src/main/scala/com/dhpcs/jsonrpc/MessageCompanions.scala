package com.dhpcs.jsonrpc

import com.dhpcs.jsonrpc.Message.MessageFormat
import com.dhpcs.jsonrpc.ResponseCompanion.ErrorResponse
import play.api.libs.json._

import scala.reflect.ClassTag

trait CommandCompanion[A] {
  protected[this] val CommandFormats: Seq[MessageFormat[_ <: A]]

  def read(jsonRpcRequestMessage: JsonRpcRequestMessage): Option[JsResult[A]] =
    CommandFormats.find(_.method == jsonRpcRequestMessage.method).map(
      typeChoiceMapping => jsonRpcRequestMessage.params.fold[JsResult[A]](
        ifEmpty = JsError("command parameters must be given")
      )(_.fold(
        _ => JsError("command parameters must be named"),
        jsObject => typeChoiceMapping.fromJson(jsObject)
      ))
    ).map(_.fold(
      // We do this in order to drop any non-root path that may have existed in the success case.
      invalid => JsError(invalid),
      valid => JsSuccess(valid)
    ))

  def write(command: A, id: Option[Either[String, BigDecimal]]): JsonRpcRequestMessage = {
    val mapping = CommandFormats.find(_.matchesInstance(command))
      .getOrElse(sys.error(s"No format found for ${command.getClass}"))
    JsonRpcRequestMessage(mapping.method, Some(Right(mapping.toJson(command).asInstanceOf[JsObject])), id)
  }
}

trait ResponseCompanion[A] {
  protected[this] val ResponseFormats: Seq[MessageFormat[_ <: A]]

  def read(jsonRpcResponseMessage: JsonRpcResponseMessage, method: String): JsResult[Either[ErrorResponse, A]] =
    jsonRpcResponseMessage.eitherErrorOrResult.fold(
      error => JsSuccess(ErrorResponse(error.code, error.message, error.data)).map(Left(_)),
      result => ResponseFormats.find(_.method == method).get.fromJson(result).map(Right(_))
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
  protected[this] val NotificationFormats: Seq[MessageFormat[_ <: A]]

  def read(jsonRpcNotificationMessage: JsonRpcNotificationMessage): Option[JsResult[A]] =
    NotificationFormats.find(_.method == jsonRpcNotificationMessage.method).map(
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
    JsonRpcNotificationMessage(mapping.method, Right(mapping.toJson(notification).asInstanceOf[JsObject]))
  }
}

object Message {

  implicit class MessageFormat[A](methodAndFormat: (String, Format[A]))
                                 (implicit val classTag: ClassTag[A]) {
    val (method, format) = methodAndFormat

    def fromJson(json: JsValue): JsResult[A] = format.reads(json)

    def matchesInstance(o: Any): Boolean = classTag.runtimeClass.isInstance(o)

    def toJson(o: Any): JsValue = format.writes(o.asInstanceOf[A])
  }

  object MessageFormats {
    def apply[A](messageFormats: MessageFormat[_ <: A]*): Seq[MessageFormat[_ <: A]] = {
      val methods = messageFormats.map(_.method)
      require(
        methods == methods.distinct,
        "Duplicate methods: " + methods.mkString(", ")
      )
      val overlappingTypes = messageFormats.combinations(2).filter {
        case Seq(first, second) => first.classTag.runtimeClass isAssignableFrom second.classTag.runtimeClass
      }
      require(
        overlappingTypes.isEmpty,
        "Overlapping types: " + overlappingTypes.map {
          case Seq(first, second) => s"${first.classTag} is assignable from ${second.classTag}"
        }.mkString(", ")
      )
      messageFormats
    }
  }

  def objectFormat[A](o: A): OFormat[A] = OFormat(
    _.validate[JsObject].map(_ => o),
    _ => Json.obj()
  )
}
