/*
 * Copyright (C) 2015 David Piggott <https://www.dhpcs.com>
 */
package com.dhpcs.jsonrpc

import com.dhpcs.json.FormatBehaviors
import org.scalatest._
import play.api.data.validation.ValidationError
import play.api.libs.json._

class JsonRpcMessageSpec extends FunSpec with FormatBehaviors[JsonRpcMessage] with Matchers {

  describe("An arbitrary JsValue") {
    it should behave like readError[JsonRpcMessage](
      Json.parse( """{}"""),
      JsError(List((__, List(ValidationError("not a valid request, request batch, response, response batch or notification message")))))
    )
  }

  describe("A JsonRpcRequestMessage") {
    describe("with an incorrect version") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"jsonrpc":"3.0","method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    }
    describe("with version of the wrong type") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"jsonrpc":2.0,"method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a version") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with method of the wrong type") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"jsonrpc":"2.0","method":3.0,"params":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "method", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a method") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"jsonrpc":"2.0","params":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "method", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with params of the wrong type") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":"params","id":0}"""),
        JsError(List((__ \ "params", List(ValidationError("error.expected.jsarray")))))
      )
    }
    describe("without params") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"jsonrpc":"2.0","method":"testMethod","id":0}"""),
        JsError(List((__ \ "params", List(ValidationError("error.path.missing")))))
      )
    }
    describe("without an id") {
      it should behave like readError[JsonRpcRequestMessage](
        Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"}}"""),
        JsError(List((__ \ "id", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with a params array") {
      describe("and a null id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Left(JsArray(
            Seq(
              JsString("param1"),
              JsString("param2")
            )
          )),
          None
        )
        implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":["param1","param2"],"id":null}""")
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Left(JsArray(
            Seq(
              JsString("param1"),
              JsString("param2")
            )
          )),
          Some(Left("one"))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":["param1","param2"],"id":"one"}""")
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Left(JsArray(
            Seq(
              JsString("param1"),
              JsString("param2")
            )
          )),
          Some(Right(1))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":["param1","param2"],"id":1}""")
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
            "testMethod",
            Left(JsArray(
              Seq(
                JsString("param1"),
                JsString("param2")
              )
            )),
            Some(Right(1.1))
          )
          implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":["param1","param2"],"id":1.1}""")
          it should behave like read
          it should behave like write
        }
      }
    }
    describe("with a params object") {
      describe("and a null id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          None
        )
        implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":null}""")
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Some(Left("one"))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":"one"}""")
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
          "testMethod",
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Some(Right(1))
        )
        implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":1}""")
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
            "testMethod",
            Right(JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )),
            Some(Right(1.1))
          )
          implicit val jsonRpcRequestMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":1.1}""")
          it should behave like read
          it should behave like write
        }
      }
    }
  }

  describe("A JsonRpcRequestMessageBatch") {
    describe("with no content") {
      it should behave like readError[JsonRpcRequestMessageBatch](
        Json.parse( """[]"""),
        JsError(List((__, List(ValidationError("error.invalid")))))
      )
    }
    describe("with an invalid request") {
      it should behave like readError[JsonRpcRequestMessageBatch](
        Json.parse( """[{"jsonrpc":"2.0","method":"testMethod","id":1}]"""),
        JsError(List((__(0) \ "params", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with a single request") {
      implicit val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Right(JsonRpcRequestMessage(
            "testMethod",
            Right(JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )),
            Some(Right(1))
          ))
        )
      )
      implicit val jsonRpcRequestMessageBatchJson = Json.parse( """[{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"},"id":1}]""")
      it should behave like read
      it should behave like write
    }
    describe("with an invalid notification") {
      it should behave like readError[JsonRpcRequestMessageBatch](
        Json.parse( """[{"jsonrpc":"2.0","method":"testMethod"}]"""),
        JsError(List((__(0) \ "params", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with a single notification") {
      implicit val jsonRpcRequestMessageBatch = JsonRpcRequestMessageBatch(
        Seq(
          Left(JsonRpcNotificationMessage(
            "testMethod",
            Right(JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            ))
          )
          )
        )
      )
      implicit val jsonRpcRequestMessageBatchJson = Json.parse( """[{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"}}]""")
      it should behave like read
      it should behave like write
    }
  }

  describe("A JsonRpcResponseMessage") {
    describe("with an incorrect version") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse( """{"jsonrpc":"3.0","result":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    }
    describe("with version of the wrong type") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse( """{"jsonrpc":2.0,"result":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a version") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse( """{"result":{"param1":"param1","param2":"param2"},"id":0}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with an error of the wrong type") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse( """{"jsonrpc":"2.0","error":"error","id":0}"""),
        JsError(List(
          (__ \ "error" \ "code", List(ValidationError("error.path.missing"))),
          (__ \ "error" \ "message", List(ValidationError("error.path.missing")))
        ))
      )
    }
    describe("without an error or a result") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse( """{"jsonrpc":"2.0","id":0}"""),
        JsError(List((__ \ "error", List(ValidationError("error.path.missing")))))
      )
    }
    describe("without an id") {
      it should behave like readError[JsonRpcResponseMessage](
        Json.parse( """{"jsonrpc":"2.0","result":{"param1":"param1","param2":"param2"}}"""),
        JsError(List((__ \ "id", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with an error") {
      describe("and a null id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Left(JsonRpcResponseError.internalError(None)),
          None
        )
        implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},"id":null}""")
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Left(JsonRpcResponseError.internalError(None)),
          Some(Left("one"))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},"id":"one"}""")
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Left(JsonRpcResponseError.internalError(None)),
          Some(Right(1))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},"id":1}""")
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
            Left(JsonRpcResponseError.internalError(None)),
            Some(Right(1.1))
          )
          implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","error":{"code":-32603,"message":"Invalid params","data":{"meaning":"Internal JSON-RPC error."}},"id":1.1}""")
          it should behave like read
          it should behave like write
        }
      }
    }
    describe("with a result") {
      describe("and a null id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          None
        )
        implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","result":{"param1":"param1","param2":"param2"},"id":null}""")
        it should behave like read
        it should behave like write
      }
      describe("and a string id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Some(Left("one"))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","result":{"param1":"param1","param2":"param2"},"id":"one"}""")
        it should behave like read
        it should behave like write
      }
      describe("and a numeric id") {
        implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
          Right(JsObject(
            Seq(
              "param1" -> JsString("param1"),
              "param2" -> JsString("param2")
            )
          )),
          Some(Right(1))
        )
        implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","result":{"param1":"param1","param2":"param2"},"id":1}""")
        it should behave like read
        it should behave like write
        describe("with a fractional part") {
          implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
            Right(JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )),
            Some(Right(1.1))
          )
          implicit val jsonRpcResponseMessageJson = Json.parse( """{"jsonrpc":"2.0","result":{"param1":"param1","param2":"param2"},"id":1.1}""")
          it should behave like read
          it should behave like write
        }
      }
    }
  }

  describe("A JsonRpcResponseMessageBatch") {
    describe("with no content") {
      it should behave like readError[JsonRpcResponseMessageBatch](
        Json.parse( """[]"""),
        JsError(List((__, List(ValidationError("error.invalid")))))
      )
    }
    describe("with an invalid response") {
      it should behave like readError[JsonRpcResponseMessageBatch](
        Json.parse( """[{"jsonrpc":"2.0","id":1}]"""),
        JsError(List((__(0) \ "error", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with a single response") {
      implicit val jsonRpcResponseMessageBatch = JsonRpcResponseMessageBatch(
        Seq(
          JsonRpcResponseMessage(
            Right(JsObject(
              Seq(
                "param1" -> JsString("param1"),
                "param2" -> JsString("param2")
              )
            )),
            Some(Right(1))
          )
        )
      )
      implicit val jsonRpcResponseMessageBatchJson = Json.parse( """[{"jsonrpc":"2.0","result":{"param1":"param1","param2":"param2"},"id":1}]""")
      it should behave like read
      it should behave like write
    }
  }

  describe("A JsonRpcNotificationMessage") {
    describe("with an incorrect version") {
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse( """{"jsonrpc":"3.0","method":"testMethod","params":{"param1":"param1","param2":"param2"}}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.invalid")))))
      )
    }
    describe("with version of the wrong type") {
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse( """{"jsonrpc":2.0,"method":"testMethod","params":{"param1":"param1","param2":"param2"}}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a version") {
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse( """{"method":"testMethod","params":{"param1":"param1","param2":"param2"}}"""),
        JsError(List((__ \ "jsonrpc", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with method of the wrong type") {
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse( """{"jsonrpc":"2.0","method":3.0,"params":{"param1":"param1","param2":"param2"}}"""),
        JsError(List((__ \ "method", List(ValidationError("error.expected.jsstring")))))
      )
    }
    describe("without a method") {
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse( """{"jsonrpc":"2.0","params":{"param1":"param1","param2":"param2"}}"""),
        JsError(List((__ \ "method", List(ValidationError("error.path.missing")))))
      )
    }
    describe("with params of the wrong type") {
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":"params"}"""),
        JsError(List((__ \ "params", List(ValidationError("error.expected.jsarray")))))
      )
    }
    describe("without params") {
      it should behave like readError[JsonRpcNotificationMessage](
        Json.parse( """{"jsonrpc":"2.0","method":"testMethod"}"""),
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
      implicit val jsonRpcNotificationMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":["param1","param2"]}""")
      it should behave like read
      it should behave like write
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
      implicit val jsonRpcNotificationMessageJson = Json.parse( """{"jsonrpc":"2.0","method":"testMethod","params":{"param1":"param1","param2":"param2"}}""")
      it should behave like read
      it should behave like write
    }
  }

}
