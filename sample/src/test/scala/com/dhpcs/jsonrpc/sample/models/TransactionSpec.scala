package com.dhpcs.jsonrpc.sample.models

import com.dhpcs.json.FormatBehaviors
import org.scalatest._
import play.api.data.validation.ValidationError
import play.api.libs.json._

class TransactionSpec extends FunSpec with FormatBehaviors[Transaction] with Matchers {

  describe("A JsValue of the wrong type") {
    it should behave like readError(
      Json.parse( """0"""),
      JsError(List(
        (__ \ "from", List(ValidationError("error.path.missing"))),
        (__ \ "to", List(ValidationError("error.path.missing"))),
        (__ \ "value", List(ValidationError("error.path.missing"))),
        (__ \ "created", List(ValidationError("error.path.missing")))
      ))
    )
  }

  describe("A Transaction") {
    describe("without a description or metadata") {
      implicit val transaction = Transaction(
        0,
        1,
        BigDecimal(1000000),
        1434115187612L
      )
      implicit val transactionJson = Json.parse( """{"from":0,"to":1,"value":1000000,"created":1434115187612}""")
      it should behave like read
      it should behave like write
    }
    describe("with a description and metadata") {
      implicit val transaction = Transaction(
        0,
        1,
        BigDecimal(1000000),
        1434115187612L,
        Some("Property purchase"),
        Some(
          Json.obj(
            "property" -> "The TARDIS"
          )
        )
      )
      implicit val transactionJson = Json.parse( """{"from":0,"to":1,"value":1000000,"created":1434115187612,"description":"Property purchase","metadata":{"property":"The TARDIS"}}""")
      it should behave like read
      it should behave like write
    }
  }

}