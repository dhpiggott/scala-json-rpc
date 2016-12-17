package com.dhpcs.jsonrpc.example.models

import com.dhpcs.json.JsResultUniformity
import com.dhpcs.jsonrpc.ResponseCompanion.ErrorResponse
import com.dhpcs.jsonrpc.{JsonRpcNotificationMessage, JsonRpcRequestMessage, JsonRpcResponseMessage}
import org.scalatest.OptionValues._
import org.scalatest._
import play.api.data.validation.ValidationError
import play.api.libs.json._

class MessageSpec extends FunSpec with Matchers {

  describe("A Command") {
    describe("with an invalid method")(
      it should behave like commandReadError(
        JsonRpcRequestMessage(
          method = "invalidMethod",
          params = Some(Right(Json.obj())),
          id = Some(Right(1))
        ),
        None
      )
    )
    describe("of type AddTransactionCommand") {
      describe("with params of the wrong type")(
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            method = "addTransaction",
            params = Some(Left(Json.arr())),
            id = Some(Right(1))
          ),
          Some(
            JsError(
              List(
                (__, List(ValidationError("command parameters must be named")))
              )
            ))
        )
      )
      describe("with empty params")(
        it should behave like commandReadError(
          JsonRpcRequestMessage(
            method = "addTransaction",
            params = Some(Right(Json.obj())),
            id = Some(Right(1))
          ),
          Some(
            JsError(
              List(
                (__ \ "from", List(ValidationError("error.path.missing"))),
                (__ \ "to", List(ValidationError("error.path.missing"))),
                (__ \ "value", List(ValidationError("error.path.missing")))
              )
            ))
        )
      )
      implicit val addTransactionCommand = AddTransactionCommand(
        from = 0,
        to = 1,
        value = BigDecimal(1000000),
        description = Some("Property purchase"),
        metadata = Some(
          Json.obj(
            "property" -> "The TARDIS"
          )
        )
      )
      implicit val id = Right(BigDecimal(1))
      implicit val jsonRpcRequestMessage = JsonRpcRequestMessage(
        "addTransaction",
        Some(
          Right(
            Json.obj(
              "from"        -> 0,
              "to"          -> 1,
              "value"       -> BigDecimal(1000000),
              "description" -> "Property purchase",
              "metadata" -> Json.obj(
                "property" -> "The TARDIS"
              )
            )
          )),
        Some(Right(1))
      )
      it should behave like commandRead
      it should behave like commandWrite
    }
  }

  describe("A Response") {
    describe("of type AddTransactionResponse")(
      describe("with empty params")(
        it should behave like responseReadError(
          JsonRpcResponseMessage(
            errorOrResult = Right(Json.obj()),
            id = Some(Right(1))
          ),
          "addTransaction",
          JsError(
            List(
              (__ \ "created", List(ValidationError("error.path.missing")))
            )
          )
        )
      )
    )
    implicit val addTransactionResponse = Right(
      AddTransactionResponse(
        created = 1434115187612L
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

  describe("A Notification") {
    describe("with an invalid method")(
      it should behave like notificationReadError(
        JsonRpcNotificationMessage(
          method = "invalidMethod",
          params = Some(Right(Json.obj()))
        ),
        jsError = None
      )
    )
    describe("of type TransactionAddedNotification") {
      describe("with params of the wrong type")(
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            method = "transactionAdded",
            params = Some(Left(Json.arr()))
          ),
          Some(
            JsError(
              List(
                (__, List(ValidationError("notification parameters must be named")))
              )
            ))
        )
      )
      describe("with empty params") {
        it should behave like notificationReadError(
          JsonRpcNotificationMessage(
            method = "transactionAdded",
            params = Some(Right(Json.obj()))
          ),
          Some(
            JsError(
              List(
                (__ \ "transaction", List(ValidationError("error.path.missing")))
              )
            ))
        )
      }
      implicit val clientJoinedZoneNotification = TransactionAddedNotification(
        Transaction(
          from = 0,
          to = 1,
          value = BigDecimal(1000000),
          created = 1434115187612L
        )
      )
      implicit val jsonRpcNotificationMessage = JsonRpcNotificationMessage(
        method = "transactionAdded",
        params = Some(
          Right(
            Json.obj(
              "transaction" -> Json.parse("""{"from":0,"to":1,"value":1000000,"created":1434115187612}""")
            )
          )
        )
      )
      it should behave like notificationRead
      it should behave like notificationWrite
    }
  }

  private[this] def commandReadError(jsonRpcRequestMessage: JsonRpcRequestMessage, jsError: Option[JsError]) =
    it(s"should fail to decode with error $jsError") {
      val jsResult = Command.read(jsonRpcRequestMessage)
      jsError.fold(jsResult shouldBe empty)(
        jsResult.value should equal(_)(after being ordered[Command])
      )
    }

  private[this] def commandRead(implicit jsonRpcRequestMessage: JsonRpcRequestMessage, command: Command) =
    it(s"should decode to $command")(
      Command.read(jsonRpcRequestMessage) should be(Some(JsSuccess(command)))
    )

  private[this] def commandWrite(implicit command: Command,
                                 id: Either[String, BigDecimal],
                                 jsonRpcRequestMessage: JsonRpcRequestMessage) =
    it(s"should encode to $jsonRpcRequestMessage")(
      Command.write(command, Some(id)) should be(jsonRpcRequestMessage)
    )

  private[this] def responseReadError(jsonRpcResponseMessage: JsonRpcResponseMessage,
                                      method: String,
                                      jsError: JsError) =
    it(s"should fail to decode with error $jsError")(
      (Response.read(jsonRpcResponseMessage, method)
        should equal(jsError))(after being ordered[Either[ErrorResponse, ResultResponse]])
    )

  private[this] def responseRead(implicit jsonRpcResponseMessage: JsonRpcResponseMessage,
                                 method: String,
                                 errorOrResponse: Either[ErrorResponse, ResultResponse]) =
    it(s"should decode to $errorOrResponse")(
      Response.read(jsonRpcResponseMessage, method) should be(JsSuccess(errorOrResponse))
    )

  private[this] def responseWrite(implicit errorOrResponse: Either[ErrorResponse, ResultResponse],
                                  id: Either[String, BigDecimal],
                                  jsonRpcResponseMessage: JsonRpcResponseMessage) =
    it(s"should encode to $jsonRpcResponseMessage")(
      Response.write(errorOrResponse, Some(id)) should be(jsonRpcResponseMessage)
    )

  private[this] def notificationReadError(jsonRpcNotificationMessage: JsonRpcNotificationMessage,
                                          jsError: Option[JsError]) =
    it(s"should fail to decode with error $jsError") {
      val notificationJsResult = Notification.read(jsonRpcNotificationMessage)
      jsError.fold(notificationJsResult shouldBe empty)(
        notificationJsResult.value should equal(_)(after being ordered[Notification])
      )
    }

  private[this] def notificationRead(implicit jsonRpcNotificationMessage: JsonRpcNotificationMessage,
                                     notification: Notification) =
    it(s"should decode to $notification")(
      Notification.read(jsonRpcNotificationMessage) should be(Some(JsSuccess(notification)))
    )

  private[this] def notificationWrite(implicit notification: Notification,
                                      jsonRpcNotificationMessage: JsonRpcNotificationMessage) =
    it(s"should encode to $jsonRpcNotificationMessage")(
      Notification.write(notification) should be(jsonRpcNotificationMessage)
    )

  private[this] def ordered[A] = new JsResultUniformity[A]

}
