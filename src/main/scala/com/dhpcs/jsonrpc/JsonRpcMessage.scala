package com.dhpcs.jsonrpc

import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import scala.collection.Seq

// TODO: Extract as separate library
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

  implicit val IdFormat = eitherValueFormat[String, Int]
  implicit val ParamsFormat = eitherValueFormat[JsArray, JsObject]

  def eitherObjectFormat[L: Format, R: Format](leftKey: String, rightKey: String) = OFormat[Either[L, R]](

    (__ \ leftKey).read[L].map(a => Left(a): Either[L, R]) orElse
      (__ \ rightKey).read[R].map(b => Right(b): Either[L, R]),

    OWrites[Either[L, R]] {
      case Left(leftValue) => Json.obj(leftKey -> leftValue)
      case Right(rightValue) => Json.obj(rightKey -> rightValue)
    }

  )

  def eitherValueFormat[L: Format, R: Format]: Format[Either[L, R]] = Format[Either[L, R]](

    __.read[L].map(a => Left(a): Either[L, R]) orElse
      __.read[R].map(b => Right(b): Either[L, R]),

    Writes[Either[L, R]] {
      case Left(leftValue) => Json.toJson[L](leftValue)
      case Right(rightValue) => Json.toJson[R](rightValue)
    }

  )

}

case class JsonRpcRequestMessage(method: String,
                                 params: Either[JsArray, JsObject],
                                 id: Either[String, Int]) extends JsonRpcMessage

object JsonRpcRequestMessage extends JsonRpcMessageCompanion {

  implicit val JsonRpcRequestMessageFormat: Format[JsonRpcRequestMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      (__ \ "method").format[String] and
      (__ \ "params").format[Either[JsArray, JsObject]] and
      (__ \ "id").format[Either[String, Int]]
    )((_, method, params, id) =>
    JsonRpcRequestMessage(method, params, id),
      jsonRpcRequestMessage =>
        (JsonRpcMessage.Version,
          jsonRpcRequestMessage.method,
          jsonRpcRequestMessage.params,
          jsonRpcRequestMessage.id)
    )

}

/**
 * Do not construct these directly - use the helpers on the companion object. As well as helping with the formatting
 * of the error content, these will ensure the correct codes are used.
 */
case class JsonRpcResponseError(code: Int,
                                message: String,
                                data: Option[JsValue])

object JsonRpcResponseError {

  implicit val JsonRpcResponseErrorFormat = Json.format[JsonRpcResponseError]

  val ReservedErrorCodeFloor = -32768
  val ReservedErrorCodeCeiling = -32000

  val ParseErrorCode = -32700
  val InvalidRequestCode = -32600
  val MethodNotFoundCode = -32601
  val InvalidParamsCode = -32602
  val InternalErrorCode = -32603
  val ServerErrorCodeFloor = -32099
  val ServerErrorCodeCeiling = -32000

  private def build(code: Int,
                    message: String,
                    meaning: String,
                    error: Option[JsValue],
                    jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = JsonRpcResponseError(
    code,
    message,
    Some(
      jsObjectBuilder(
        ("meaning" -> JsString(meaning)) ::
          error.fold[List[(String, JsValue)]](Nil)(
            error => List("error" -> error)
          )
      )
    )
  )

  def parseError(exception: Throwable,
                 jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = build(
    ParseErrorCode,
    "Parse error",
    "Invalid JSON was received by the server.\nAn error occurred on the server while parsing the JSON text.",
    Some(JsString(exception.getMessage)),
    jsObjectBuilder
  )

  def invalidRequest(errors: Seq[(JsPath, Seq[ValidationError])],
                     jsErrorObjectBuilder: Seq[(JsPath, Seq[ValidationError])] => JsObject,
                     jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = build(
    InvalidRequestCode,
    "Invalid Request",
    "The JSON sent is not a valid Request object.",
    Some(jsErrorObjectBuilder(errors)),
    jsObjectBuilder
  )

  def methodNotFound(method: String,
                     jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = build(
    MethodNotFoundCode,
    "Method not found",
    "The method does not exist / is not available.",
    Some(JsString( s"""The method "$method" is not implemented.""")),
    jsObjectBuilder
  )

  def invalidParams(errors: Seq[(JsPath, Seq[ValidationError])],
                    jsErrorObjectBuilder: Seq[(JsPath, Seq[ValidationError])] => JsObject,
                    jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = build(
    InvalidParamsCode,
    "Invalid params",
    "Invalid method toFlatJson(s).",
    Some(jsErrorObjectBuilder(errors)),
    jsObjectBuilder
  )

  def internalError(error: Option[JsValue] = None,
                    jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = build(
    InternalErrorCode,
    "Invalid params",
    "Internal JSON-RPC error.",
    error,
    jsObjectBuilder
  )

  def serverError(code: Int, error: Option[JsValue] = None,
                  jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = {
    require(code >= ServerErrorCodeFloor && code <= ServerErrorCodeCeiling)
    build(
      InternalErrorCode,
      "Invalid params",
      "Internal JSON-RPC error.",
      error,
      jsObjectBuilder
    )
  }

  def applicationError(code: Int,
                       message: String,
                       meaning: String,
                       error: Option[JsValue] = None,
                       jsObjectBuilder: Seq[(String, JsValue)] => JsObject) = {
    require(code > ReservedErrorCodeCeiling || code < ReservedErrorCodeFloor)
    build(
      code,
      message,
      meaning,
      error,
      jsObjectBuilder
    )
  }

}

case class JsonRpcResponseMessage(eitherErrorOrResult: Either[JsonRpcResponseError, JsValue],
                                  id: Option[Either[String, Int]]) extends JsonRpcMessage

object JsonRpcResponseMessage extends JsonRpcMessageCompanion {

  implicit val JsonRpcResponseMessageFormat: Format[JsonRpcResponseMessage] = (
    (__ \ "jsonrpc").format(verifying[String](_ == JsonRpcMessage.Version)) and
      __.format(eitherObjectFormat[JsonRpcResponseError, JsValue]("error", "result")) and
      (__ \ "id").format(Format.optionWithNull[Either[String, Int]])
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