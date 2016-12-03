package com.dhpcs.jsonrpc

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Reads.verifying
import play.api.libs.json._

import scala.collection.Seq

sealed trait JsonRpcMessage

object JsonRpcMessage {

  final val Version = "2.0"

  implicit final val JsonRpcMessageFormat: Format[JsonRpcMessage] = new Format[JsonRpcMessage] {
    override def reads(json: JsValue) =
      (
        __.read(JsonRpcRequestMessage.JsonRpcRequestMessageFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcRequestMessageBatch.JsonRpcRequestMessageBatchFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcResponseMessage.JsonRpcResponseMessageFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcResponseMessageBatch.JsonRpcResponseMessageBatchFormat).map(m => m: JsonRpcMessage) orElse
          __.read(JsonRpcNotificationMessage.JsonRpcNotificationMessageFormat).map(m => m: JsonRpcMessage)
      ).reads(json)
        .orElse(
          JsError("not a valid request, request batch, response, response batch or notification message")
        )

    override def writes(jsonRpcMessage: JsonRpcMessage) = jsonRpcMessage match {
      case jsonRpcRequestMessage: JsonRpcRequestMessage =>
        Json.toJson(jsonRpcRequestMessage)(JsonRpcRequestMessage.JsonRpcRequestMessageFormat)
      case jsonRpcRequestMessageBatch: JsonRpcRequestMessageBatch =>
        Json.toJson(jsonRpcRequestMessageBatch)(JsonRpcRequestMessageBatch.JsonRpcRequestMessageBatchFormat)
      case jsonRpcResponseMessage: JsonRpcResponseMessage =>
        Json.toJson(jsonRpcResponseMessage)(JsonRpcResponseMessage.JsonRpcResponseMessageFormat)
      case jsonRpcResponseMessageBatch: JsonRpcResponseMessageBatch =>
        Json.toJson(jsonRpcResponseMessageBatch)(JsonRpcResponseMessageBatch.JsonRpcResponseMessageBatchFormat)
      case jsonRpcNotificationMessage: JsonRpcNotificationMessage =>
        Json.toJson(jsonRpcNotificationMessage)(JsonRpcNotificationMessage.JsonRpcNotificationMessageFormat)
    }
  }
}

case class JsonRpcRequestMessage(method: String,
                                 params: Option[Either[JsArray, JsObject]],
                                 id: Option[Either[String, BigDecimal]])
    extends JsonRpcMessage

object JsonRpcRequestMessage extends JsonRpcMessageCompanion {
  implicit final val JsonRpcRequestMessageFormat: Format[JsonRpcRequestMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      (__ \ "method").format[String] and
      (__ \ "params").format[Option[Either[JsArray, JsObject]]] and
      (__ \ "id").format(Format.optionWithNull[Either[String, BigDecimal]])
  )((_, method, params, id) => JsonRpcRequestMessage(method, params, id),
    jsonRpcRequestMessage =>
      (JsonRpcMessage.Version, jsonRpcRequestMessage.method, jsonRpcRequestMessage.params, jsonRpcRequestMessage.id))
}

case class JsonRpcRequestMessageBatch(messages: Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]])
    extends JsonRpcMessage {
  require(messages.nonEmpty)
}

object JsonRpcRequestMessageBatch extends JsonRpcMessageCompanion {

  implicit final val RequestOrNotificationFormat: Format[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]] =
    eitherValueFormat[JsonRpcNotificationMessage, JsonRpcRequestMessage]

  implicit final val JsonRpcRequestMessageBatchFormat: Format[JsonRpcRequestMessageBatch] = Format(
    Reads(
      json =>
        Reads
          .of[Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]]](verifying(_.nonEmpty))
          .reads(json)
          .map(JsonRpcRequestMessageBatch(_))
    ),
    Writes(
      a => Writes.of[Seq[Either[JsonRpcNotificationMessage, JsonRpcRequestMessage]]].writes(a.messages)
    )
  )
}

case class JsonRpcResponseMessage(eitherErrorOrResult: Either[JsonRpcResponseError, JsValue],
                                  id: Option[Either[String, BigDecimal]])
    extends JsonRpcMessage

object JsonRpcResponseMessage extends JsonRpcMessageCompanion {
  implicit final val JsonRpcResponseMessageFormat: Format[JsonRpcResponseMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      __.format(eitherObjectFormat[JsonRpcResponseError, JsValue]("error", "result")) and
      (__ \ "id").format(Format.optionWithNull[Either[String, BigDecimal]])
  )((_, eitherErrorOrResult, id) => JsonRpcResponseMessage(eitherErrorOrResult, id),
    jsonRpcResponseMessage =>
      (JsonRpcMessage.Version, jsonRpcResponseMessage.eitherErrorOrResult, jsonRpcResponseMessage.id))
}

case class JsonRpcResponseMessageBatch(messages: Seq[JsonRpcResponseMessage]) extends JsonRpcMessage {
  require(messages.nonEmpty)
}

object JsonRpcResponseMessageBatch extends JsonRpcMessageCompanion {
  implicit final val JsonRpcResponseMessageBatchFormat: Format[JsonRpcResponseMessageBatch] = Format(
    Reads(
      json =>
        Reads.of[Seq[JsonRpcResponseMessage]](verifying(_.nonEmpty)).reads(json).map(JsonRpcResponseMessageBatch(_))
    ),
    Writes(
      a => Writes.of[Seq[JsonRpcResponseMessage]].writes(a.messages)
    )
  )
}

case class JsonRpcNotificationMessage(method: String, params: Either[JsArray, JsObject]) extends JsonRpcMessage

object JsonRpcNotificationMessage extends JsonRpcMessageCompanion {
  implicit final val JsonRpcNotificationMessageFormat: Format[JsonRpcNotificationMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      (__ \ "method").format[String] and
      (__ \ "params").format[Either[JsArray, JsObject]]
  )((_, method, params) => JsonRpcNotificationMessage(method, params),
    jsonRpcNotificationMessage =>
      (JsonRpcMessage.Version, jsonRpcNotificationMessage.method, jsonRpcNotificationMessage.params))
}

trait JsonRpcMessageCompanion {

  implicit final val IdFormat: Format[Either[String, BigDecimal]]    = eitherValueFormat[String, BigDecimal]
  implicit final val ParamsFormat: Format[Either[JsArray, JsObject]] = eitherValueFormat[JsArray, JsObject]

  protected[this] def eitherObjectFormat[A: Format, B: Format](leftKey: String,
                                                               rightKey: String): Format[Either[A, B]] =
    OFormat(
      (__ \ rightKey).read[B].map(b => Right(b): Either[A, B]) orElse
        (__ \ leftKey).read[A].map(a => Left(a): Either[A, B]),
      OWrites[Either[A, B]] {
        case Right(rightValue) => Json.obj(rightKey -> Json.toJson(rightValue))
        case Left(leftValue)   => Json.obj(leftKey  -> Json.toJson[A](leftValue))
      }
    )

  protected[this] def eitherValueFormat[A: Format, B: Format]: Format[Either[A, B]] =
    Format(
      __.read[B].map(b => Right(b): Either[A, B]) orElse
        __.read[A].map(a => Left(a): Either[A, B]),
      Writes[Either[A, B]] {
        case Right(rightValue) => Json.toJson(rightValue)
        case Left(leftValue)   => Json.toJson(leftValue)
      }
    )
}

sealed abstract case class JsonRpcResponseError(code: Int, message: String, data: Option[JsValue])

object JsonRpcResponseError {

  implicit final val JsonRpcResponseErrorFormat: Format[JsonRpcResponseError] = (
    (__ \ "code").format[Int] and
      (__ \ "message").format[String] and
      (__ \ "data").formatNullable[JsValue]
  )((code, message, data) => new JsonRpcResponseError(code, message, data) {},
    jsonRpcResponseError => (jsonRpcResponseError.code, jsonRpcResponseError.message, jsonRpcResponseError.data))

  final val ReservedErrorCodeFloor   = -32768
  final val ReservedErrorCodeCeiling = -32000

  final val ParseErrorCode         = -32700
  final val InvalidRequestCode     = -32600
  final val MethodNotFoundCode     = -32601
  final val InvalidParamsCode      = -32602
  final val InternalErrorCode      = -32603
  final val ServerErrorCodeFloor   = -32099
  final val ServerErrorCodeCeiling = -32000

  def parseError(exception: Throwable): JsonRpcResponseError = rpcError(
    ParseErrorCode,
    "Parse error",
    "Invalid JSON was received by the server.\nAn error occurred on the server while parsing the JSON text.",
    Some(JsString(exception.getMessage))
  )

  def invalidRequest(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError = rpcError(
    InvalidRequestCode,
    "Invalid Request",
    "The JSON sent is not a valid Request object.",
    Some(JsError.toFlatJson(errors))
  )

  def methodNotFound(method: String): JsonRpcResponseError = rpcError(
    MethodNotFoundCode,
    "Method not found",
    "The method does not exist / is not available.",
    Some(JsString(s"""The method "$method" is not implemented."""))
  )

  def invalidParams(errors: Seq[(JsPath, Seq[ValidationError])]): JsonRpcResponseError = rpcError(
    InvalidParamsCode,
    "Invalid params",
    "Invalid method parameter(s).",
    Some(JsError.toFlatJson(errors))
  )

  def internalError(error: Option[JsValue] = None): JsonRpcResponseError = rpcError(
    InternalErrorCode,
    "Invalid params",
    "Internal JSON-RPC error.",
    error
  )

  def serverError(code: Int, error: Option[JsValue] = None): JsonRpcResponseError = {
    require(code >= ServerErrorCodeFloor && code <= ServerErrorCodeCeiling)
    rpcError(
      InternalErrorCode,
      "Invalid params",
      "Internal JSON-RPC error.",
      error
    )
  }

  private def rpcError(code: Int, message: String, meaning: String, error: Option[JsValue]): JsonRpcResponseError =
    new JsonRpcResponseError(
      code,
      message,
      Some(
        Json.obj(
          ("meaning" -> meaning: (String, JsValueWrapper)) ::
            error.fold[List[(String, JsValueWrapper)]](ifEmpty = Nil)(
              error => List("error" -> error)
            ): _*
        )
      )
    ) {}

  def applicationError(code: Int, message: String, data: Option[JsValue] = None): JsonRpcResponseError = {
    require(code > ReservedErrorCodeCeiling || code < ReservedErrorCodeFloor)
    new JsonRpcResponseError(
      code,
      message,
      data
    ) {}
  }
}
