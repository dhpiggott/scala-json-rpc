/*
 * Copyright (C) 2015 David Piggott <https://www.dhpcs.com>
 */
package com.dhpcs.jsonrpc

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.collection.Seq

sealed trait JsonRpcMessage

object JsonRpcMessage {

  val Version = "2.0"

  implicit val JsonRpcMessageFormat: Format[JsonRpcMessage] = new Format[JsonRpcMessage] {

    override def reads(jsValue: JsValue) = (
      __.read[JsonRpcRequestMessage].map(m => m: JsonRpcMessage) orElse
        __.read[JsonRpcResponseMessage].map(m => m: JsonRpcMessage) orElse
        __.read[JsonRpcNotificationMessage].map(m => m: JsonRpcMessage)
      ).reads(jsValue).orElse(JsError("not a valid request, response or notification message"))

    override def writes(jsonRpcMessage: JsonRpcMessage) = jsonRpcMessage match {
      case jsonRpcRequestMessage: JsonRpcRequestMessage =>
        Json.toJson(jsonRpcRequestMessage)(JsonRpcRequestMessage.JsonRpcRequestMessageFormat)
      case jsonRpcResponseMessage: JsonRpcResponseMessage =>
        Json.toJson(jsonRpcResponseMessage)(JsonRpcResponseMessage.JsonRpcResponseMessageFormat)
      case jsonRpcNotificationMessage: JsonRpcNotificationMessage =>
        Json.toJson(jsonRpcNotificationMessage)(JsonRpcNotificationMessage.JsonRpcNotificationMessageFormat)
    }

  }

}

abstract class JsonRpcMessageCompanion {

  implicit val IdFormat = eitherValueFormat[String, BigDecimal]
  implicit val ParamsFormat = eitherValueFormat[JsArray, JsObject]

  def eitherObjectFormat[L, R](leftKey: String, rightKey: String)
                              (implicit leftFormat: Format[L], rightFormat: Format[R]) =
    OFormat(

      (__ \ rightKey).read(rightFormat).map(b => Right(b): Either[L, R]) orElse
        (__ \ leftKey).read(leftFormat).map(a => Left(a): Either[L, R]),

      OWrites[Either[L, R]] {
        case Right(rightValue) => Json.obj(rightKey -> rightFormat.writes(rightValue))
        case Left(leftValue) => Json.obj(leftKey -> leftFormat.writes(leftValue))
      }

    )

  def eitherValueFormat[L, R](implicit leftFormat: Format[L], rightFormat: Format[R]) =
    Format(

      __.read(rightFormat).map(b => Right(b): Either[L, R]) orElse
        __.read(leftFormat).map(a => Left(a): Either[L, R]),

      Writes[Either[L, R]] {
        case Right(rightValue) => rightFormat.writes(rightValue)
        case Left(leftValue) => leftFormat.writes(leftValue)
      }

    )

}

case class JsonRpcRequestMessage(method: String,
                                 params: Either[JsArray, JsObject],
                                 id: Either[String, BigDecimal]) extends JsonRpcMessage

object JsonRpcRequestMessage extends JsonRpcMessageCompanion {

  implicit val JsonRpcRequestMessageFormat: Format[JsonRpcRequestMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      (__ \ "method").format[String] and
      (__ \ "params").format[Either[JsArray, JsObject]] and
      (__ \ "id").format[Either[String, BigDecimal]]
    )((_, method, params, id) =>
    JsonRpcRequestMessage(method, params, id),
      jsonRpcRequestMessage =>
        (JsonRpcMessage.Version,
          jsonRpcRequestMessage.method,
          jsonRpcRequestMessage.params,
          jsonRpcRequestMessage.id)
    )

}

sealed trait JsonRpcResponseError {

  def code: Int

  def message: String

  def data: Option[JsValue]

}

object JsonRpcResponseError {

  private case class RealJsonRpcResponseError(code: Int,
                                              message: String,
                                              data: Option[JsValue]) extends JsonRpcResponseError

  private val RealJsonRpcResponseErrorFormat = Json.format[RealJsonRpcResponseError]

  implicit val JsonRpcResponseErrorFormat: Format[JsonRpcResponseError] = Format(
    Reads(json =>
      RealJsonRpcResponseErrorFormat.reads(json).map(
        realJsonRpcResponseError => realJsonRpcResponseError: JsonRpcResponseError
      )),
    Writes(traitJsonRpcResponseError =>
      RealJsonRpcResponseErrorFormat.writes(
        traitJsonRpcResponseError.asInstanceOf[RealJsonRpcResponseError]
      ))
  )

  val ReservedErrorCodeFloor = -32768
  val ReservedErrorCodeCeiling = -32000

  val ParseErrorCode = -32700
  val InvalidRequestCode = -32600
  val MethodNotFoundCode = -32601
  val InvalidParamsCode = -32602
  val InternalErrorCode = -32603
  val ServerErrorCodeFloor = -32099
  val ServerErrorCodeCeiling = -32000

  private def apply(code: Int,
                    message: String,
                    meaning: String,
                    error: Option[JsValue]): JsonRpcResponseError = RealJsonRpcResponseError(
    code,
    message,
    Some(
      Json.obj(
        ("meaning" -> meaning: (String, JsValueWrapper)) ::
          error.fold[List[(String, JsValueWrapper)]](Nil)(
            error => List("error" -> error)
          ): _*
      )
    )
  )

  private def apply(code: Int,
                    message: String,
                    data: Option[JsValue]): JsonRpcResponseError = RealJsonRpcResponseError(
    code,
    message,
    data
  )

  def parseError(exception: Throwable) = JsonRpcResponseError(
    ParseErrorCode,
    "Parse error",
    "Invalid JSON was received by the server.\nAn error occurred on the server while parsing the JSON text.",
    Some(JsString(exception.getMessage))
  )

  def invalidRequest(errors: Seq[(JsPath, Seq[ValidationError])]) = JsonRpcResponseError(
    InvalidRequestCode,
    "Invalid Request",
    "The JSON sent is not a valid Request object.",
    Some(JsError.toFlatJson(errors))
  )

  def methodNotFound(method: String) = JsonRpcResponseError(
    MethodNotFoundCode,
    "Method not found",
    "The method does not exist / is not available.",
    Some(JsString( s"""The method "$method" is not implemented."""))
  )

  def invalidParams(errors: Seq[(JsPath, Seq[ValidationError])]) = JsonRpcResponseError(
    InvalidParamsCode,
    "Invalid params",
    "Invalid method parameter(s).",
    Some(JsError.toFlatJson(errors))
  )

  def internalError(error: Option[JsValue] = None) = JsonRpcResponseError(
    InternalErrorCode,
    "Invalid params",
    "Internal JSON-RPC error.",
    error
  )

  def serverError(code: Int, error: Option[JsValue] = None) = {
    require(code >= ServerErrorCodeFloor && code <= ServerErrorCodeCeiling)
    JsonRpcResponseError(
      InternalErrorCode,
      "Invalid params",
      "Internal JSON-RPC error.",
      error
    )
  }

  def applicationError(code: Int,
                       message: String,
                       data: Option[JsValue] = None) = {
    require(code > ReservedErrorCodeCeiling || code < ReservedErrorCodeFloor)
    JsonRpcResponseError(
      code,
      message,
      data
    )
  }

}

case class JsonRpcResponseMessage(eitherErrorOrResult: Either[JsonRpcResponseError, JsValue],
                                  id: Option[Either[String, BigDecimal]]) extends JsonRpcMessage

object JsonRpcResponseMessage extends JsonRpcMessageCompanion {

  implicit val JsonRpcResponseMessageFormat: Format[JsonRpcResponseMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      __.format(eitherObjectFormat[JsonRpcResponseError, JsValue]("error", "result")) and
      (__ \ "id").format(Format.optionWithNull[Either[String, BigDecimal]])
    )((_, eitherErrorOrResult, id) =>
    JsonRpcResponseMessage(eitherErrorOrResult, id),
      jsonRpcResponseMessage =>
        (JsonRpcMessage.Version,
          jsonRpcResponseMessage.eitherErrorOrResult,
          jsonRpcResponseMessage.id)
    )

}

case class JsonRpcNotificationMessage(method: String,
                                      params: Either[JsArray, JsObject]) extends JsonRpcMessage

object JsonRpcNotificationMessage extends JsonRpcMessageCompanion {

  implicit val JsonRpcNotificationMessageFormat: Format[JsonRpcNotificationMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      (__ \ "method").format[String] and
      (__ \ "params").format[Either[JsArray, JsObject]]
    )((_, method, params) =>
    JsonRpcNotificationMessage(method, params),
      jsonRpcNotificationMessage =>
        (JsonRpcMessage.Version,
          jsonRpcNotificationMessage.method,
          jsonRpcNotificationMessage.params)
    )

} 
