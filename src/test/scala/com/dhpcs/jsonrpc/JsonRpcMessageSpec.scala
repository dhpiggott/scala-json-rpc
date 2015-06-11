package com.dhpcs.jsonrpc

import org.scalatest._
import play.api.data.validation.ValidationError
import play.api.libs.json._

// TODO: Extract as separate library
class JsonRpcMessageSpec extends FunSpec with Matchers {

  def decodeError[T <: JsonRpcMessage : Format](badJsonRpcMessageJson: JsValue, jsError: JsError) =
    it(s"$badJsonRpcMessageJson should fail to decode with error $jsError") {
      Json.fromJson[T](badJsonRpcMessageJson) should be(jsError)
    }

  def decode[T <: JsonRpcMessage : Format](implicit jsonRpcMessageJson: JsValue, jsonRpcMessage: T) =
    it(s"$jsonRpcMessageJson should decode to $jsonRpcMessage") {
      jsonRpcMessageJson.as[T] should be(jsonRpcMessage)
    }

  def encode[T <: JsonRpcMessage : Format](implicit jsonRpcMessage: T, jsonRpcMessageJson: JsValue) =
    it(s"$jsonRpcMessage should encode to $jsonRpcMessageJson") {
      Json.toJson(jsonRpcMessage) should be(jsonRpcMessageJson)
    }

  describe("An arbitrary JsValue") {
    it should behave like decodeError[JsonRpcMessage](
      Json.parse("{}"),
      JsError(List((__, List(ValidationError("not a valid request, response or notification message")))))
    )
  }

  describe("A JsonRpcRequestMessage") {
    describe("with an incorrect version") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"jsonrpc\":\"3.0\",\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    }
    describe("with version of the wrong type") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"jsonrpc\":2.0,\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a version") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with method of the wrong type") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"method\":3.0,\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "method", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a method") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "method", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with params of the wrong type") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":\"params\",\"id\":0}"),
        JsError(List((__ \ "params", List(ValidationError("error.expected.jsobject")))))
      )
    }
    describe("without params") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"id\":0}"),
        JsError(List((__ \ "params", List(ValidationError("error.path.missing")))))
      )
    }
    describe("without an id") {
      it should behave like decodeError[JsonRpcRequestMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"}}"),
        JsError(List((__ \ "id", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with a params array") {
      describe("and an identifier string") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Left(JsArray(
            Seq(
              JsString("param1"),
              JsString("param2")
            )
          )),
          Left("zero")
        )
        implicit val jsonRpcRequestMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":[\"param1\",\"param2\"],\"id\":\"zero\"}")
        it should behave like decode
        it should behave like encode
      }
      describe("and an identifier int") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Left(JsArray(
            Seq(
              JsString("param1"),
              JsString("param2")
            )
          )),
          Right(0)
        )
        implicit val jsonRpcRequestMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":[\"param1\",\"param2\"],\"id\":0}")
        it should behave like decode
        it should behave like encode
      }
    }
    describe("with a params object") {
      describe("and an identifier string") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Left("zero")
        )
        implicit val jsonRpcRequestMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":\"zero\"}")
        it should behave like decode
        it should behave like encode
      }
      describe("and an identifier int") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Right(0)
        )
        implicit val jsonRpcRequestMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}")
        it should behave like decode
        it should behave like encode
      }
    }
  }

  describe("A JsonRpcResponseMessage") {
    describe("with an incorrect version") {
      it should behave like decodeError[JsonRpcResponseMessage](
        Json.parse("{\"jsonrpc\":\"3.0\",\"result\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    }
    describe("with version of the wrong type") {
      it should behave like decodeError[JsonRpcResponseMessage](
        Json.parse("{\"jsonrpc\":2.0,\"result\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a version") {
      it should behave like decodeError[JsonRpcResponseMessage](
        Json.parse("{\"result\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with an error of the wrong type") {
      it should behave like decodeError[JsonRpcResponseMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"error\":\"error\",\"id\":0}"),
        JsError(List((__ \ "result", List(ValidationError("error.path.missing")))))
      )
    }
    describe("without an error or a result") {
      it should behave like decodeError[JsonRpcResponseMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"id\":0}"),
        JsError(List((__ \ "result", List(ValidationError("error.path.missing")))))
      )
    }
    describe("without an id") {
      it should behave like decodeError[JsonRpcResponseMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"result\":{\"param1\":\"param1\",\"param2\":\"param2\"}}"),
        JsError(List((__ \ "id", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with an error") {
      describe("and a null identifier") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Left(JsonRpcResponseError.internalError(None, JsObject)),
          None
        )
        implicit val jsonRpcResponseMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Invalid params\",\"data\":{\"meaning\":\"Internal JSON-RPC error.\"}},\"id\":null}")
        it should behave like decode
        it should behave like encode
      }
      describe("and an identifier string") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Left(JsonRpcResponseError.internalError(None, JsObject)),
          Some(Left("zero"))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Invalid params\",\"data\":{\"meaning\":\"Internal JSON-RPC error.\"}},\"id\":\"zero\"}")
        it should behave like decode
        it should behave like encode
      }
      describe("and an identifier int") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Left(JsonRpcResponseError.internalError(None, JsObject)),
          Some(Right(0))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Invalid params\",\"data\":{\"meaning\":\"Internal JSON-RPC error.\"}},\"id\":0}")
        it should behave like decode
        it should behave like encode
      }
    }
    describe("with a result") {
      describe("and a null identifier") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          None
        )
        implicit val jsonRpcResponseMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"result\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":null}")
        it should behave like decode
        it should behave like encode
      }
      describe("and an identifier string") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Some(Left("zero"))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"result\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":\"zero\"}")
        it should behave like decode
        it should behave like encode
      }
      describe("and an identifier int") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Some(Right(0))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"result\":{\"param1\":\"param1\",\"param2\":\"param2\"},\"id\":0}")
        it should behave like decode
        it should behave like encode
      }
    }
  }

  describe("A JsonRpcNotificationMessage") {
    describe("with an incorrect version") {
      it should behave like decodeError[JsonRpcNotificationMessage](
        Json.parse("{\"jsonrpc\":\"3.0\",\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"}}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    }
    describe("with version of the wrong type") {
      it should behave like decodeError[JsonRpcNotificationMessage](
        Json.parse("{\"jsonrpc\":2.0,\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"}}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a version") {
      it should behave like decodeError[JsonRpcNotificationMessage](
        Json.parse("{\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"}}"),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with method of the wrong type") {
      it should behave like decodeError[JsonRpcNotificationMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"method\":3.0,\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"}}"),
        JsError(List((__ \ "method", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a method") {
      it should behave like decodeError[JsonRpcNotificationMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"}}"),
        JsError(List((__ \ "method", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with params of the wrong type") {
      it should behave like decodeError[JsonRpcNotificationMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":\"params\"}"),
        JsError(List((__ \ "params", List(ValidationError("error.expected.jsobject")))))
      )
    }
    describe("without params") {
      it should behave like decodeError[JsonRpcNotificationMessage](
        Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\"}"),
        JsError(List((__ \ "params", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with a params array") {
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        "testMethod",
        Left(JsArray(
          Seq(
            JsString("param1"),
            JsString("param2")
          )
        ))
      )
      implicit val jsonRpcNotificationMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":[\"param1\",\"param2\"]}")
      it should behave like decode
      it should behave like encode
    }
    describe("with a params object") {
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        "testMethod",
        Right(JsObject(
          Seq(
            "param1" -> JsString("param1"),
            "param2" -> JsString("param2")
          )
        ))
      )
      implicit val jsonRpcNotificationMessageJson = Json.parse("{\"jsonrpc\":\"2.0\",\"method\":\"testMethod\",\"params\":{\"param1\":\"param1\",\"param2\":\"param2\"}}")
      it should behave like decode
      it should behave like encode
    }
  }

}