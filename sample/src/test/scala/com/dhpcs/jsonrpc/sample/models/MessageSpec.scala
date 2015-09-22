package com.dhpcs.jsonrpc.sample.models

import com.dhpcs.json.JsResultUniformity
import com.dhpcs.jsonrpc.{ErrorResponse, JsonRpcNotificationMessage, JsonRpcRequestMessage, JsonRpcResponseMessage}
import org.scalatest.OptionValues._
import org.scalatest._
import play.api.data.validation.ValidationError
import play.api.libs.json._

class MessageSpec extends FunSpec with Matchers {

  def ordered[A] = new JsResultUniformity[A]

  def commandReadError(jsonRpcRequestMessage: JsonRpcRequestMessage, jsError: Option[JsError]) =
    it(s"should fail to decode with error $jsError") {
      val jsResult = Command.read(jsonRpcRequestMessage)
      jsError.fold(jsResult shouldBe empty)(
        jsResult.value should equal(_)(after being ordered[Command])
      )
    }

  def commandRead(implicit jsonRpcRequestMessage: JsonRpcRequestMessage, command: Command) =
    it(s"should decode to $command") {
      Command.read(jsonRpcRequestMessage) should be(Some(JsSuccess(command)))
    }

  def commandWrite(implicit command: Command,
                   id: Either[String, BigDecimal],
                   jsonRpcRequestMessage: JsonRpcRequestMessage) =
    it(s"should encode to $jsonRpcRequestMessage") {
      Command.write(command, Some(id)) should be(jsonRpcRequestMessage)
    }

  describe("A Command") {
    describe("with an invalid method") {
      it should behave like commandReadError(
        JsonRpcRequestMessage(
          "invalidMethod",
          Right(Json.obj()),
          Some(Right(1))
        ),
        None
      )
    }
    describe("of type AddTransactionCommand") {
      describe("with params of the wrong type") {
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            "addTransaction",
            Left(Json.arr()),
            Some(Right(1))
          ),
          Some(JsError(
            List(
              (__, List(ValidationError("command parameters must be named")))
            )
          ))
        )
      }
      describe("with empty params") {
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            "addTransaction",
            Right(Json.obj()),
            Some(Right(1))
          ),
          Some(JsError(
            List(
              (__ \ "from", List(ValidationError("error.path.missing"))),
              (__ \ "to", List(ValidationError("error.path.missing"))),
              (__ \ "value", List(ValidationError("error.path.missing")))
            )
          ))
        )
      }
      implicit val addTransactionCommand = AddTransactionCommand(
        0,
        1,
        BigDecimal(1000000),
        Some("Property purchase"),
        Some(
          Json.obj(
            "property" -> "The TARDIS"
          )
        )
      )
      implicit val id = Right(BigDecimal(1))
      implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
        "addTransaction",
        Right(
          Json.obj(
            "from" -> 0,
            "to" -> 1,
            "value" -> BigDecimal(1000000),
            "description" -> "Property purchase",
            "metadata" -> Json.obj(
              "property" -> "The TARDIS"
            )
          )
        ),
        Some(Right(1))
      )
      it should behave like commandRead
      it should behave like commandWrite
    }
  }

  def responseReadError(jsonRpcResponseMessage: JsonRpcResponseMessage, method: String, jsError: JsError) =
    it(s"should fail to decode with error $jsError") {
      (Response.read(jsonRpcResponseMessage, method)
        should equal(jsError))(after being ordered[Either[ErrorResponse, ResultResponse]])
    }

  def responseRead(implicit jsonRpcResponseMessage: JsonRpcResponseMessage,
                   method: String,
                   errorOrResponse: Either[ErrorResponse, ResultResponse]) =
    it(s"should decode to $errorOrResponse") {
      Response.read(jsonRpcResponseMessage, method) should be(JsSuccess(errorOrResponse))
    }

  def responseWrite(implicit errorOrResponse: Either[ErrorResponse, ResultResponse],
                    id: Either[String, BigDecimal],
                    jsonRpcResponseMessage: JsonRpcResponseMessage) =
    it(s"should encode to $jsonRpcResponseMessage") {
      Response.write(errorOrResponse, Some(id)) should be(jsonRpcResponseMessage)
    }

  describe("A Response") {
    describe("of type AddTransactionResponse") {
      describe("with empty params") {
        it should behave like responseReadError(
          JsonRpcResponseMessage(
            Right(Json.obj()),
            Some(Right(0))
          ),
          "addTransaction",
          JsError(
            List(
              (__ \ "created", List(ValidationError("error.path.missing")))
            )
          )
        )
      }
    }
    implicit val addTransactionResponse = Right(AddTransactionResponse(
      1434115187612L
    ))
    implicit val id = Right(BigDecimal(1))
    implicit val jsonRpcResponseMessage = JsonRpcResponseMessage(
      Right(
        Json.obj(
          "created" -> 1434115187612L
        )
      ),
      Some(Right(1))
    )
    implicit val method = "addTransaction"
    it should behave like responseRead
    it should behave like responseWrite
  }

  def notificationReadError(jsonRpcNotificationMessage: JsonRpcNotificationMessage, jsError: Option[JsError]) =
    it(s"should fail to decode with error $jsError") {
      val notificationJsResult = Notification.read(jsonRpcNotificationMessage)
      jsError.fold(notificationJsResult shouldBe empty)(
        notificationJsResult.value should equal(_)(after being ordered[Notification])
      )
    }

  def notificationRead(implicit jsonRpcNotificationMessage: JsonRpcNotificationMessage, notification: Notification) =
    it(s"should decode to $notification") {
      Notification.read(jsonRpcNotificationMessage) should be(Some(JsSuccess(notification)))
    }

  def notificationWrite(implicit notification: Notification, jsonRpcNotificationMessage: JsonRpcNotificationMessage) =
    it(s"should encode to $jsonRpcNotificationMessage") {
      Notification.write(notification) should be(jsonRpcNotificationMessage)
    }

  describe("A Notification") {
    describe("with an invalid method") {
      it should behave like notificationReadError(
        JsonRpcNotificationMessage(
          "invalidMethod",
          Right(Json.obj())
        ),
        None
      )
    }
    describe("of type TransactionAddedNotification") {
      describe("with params of the wrong type") {
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            "transactionAdded",
            Left(Json.arr())
          ),
          Some(JsError(
            List(
              (__, List(ValidationError("notification parameters must be named")))
            )
          ))
        )
      }
      describe("with empty params") {
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            "transactionAdded",
            Right(Json.obj())
          ),
          Some(JsError(
            List(
              (__ \ "transaction", List(ValidationError("error.path.missing")))
            )
          ))
        )
      }
      implicit val clientJoinedZoneNotification = TransactionAddedNotification(
        Transaction(
          0,
          1,
          BigDecimal(1000000),
          1434115187612L
        )
      )
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        "transactionAdded",
        Right(
          Json.obj(
            "transaction" -> Json.parse( """{"from":0,"to":1,"value":1000000,"created":1434115187612}""")
          )
        )
      )
      it should behave like notificationRead
      it should behave like notificationWrite
    }
  }

}