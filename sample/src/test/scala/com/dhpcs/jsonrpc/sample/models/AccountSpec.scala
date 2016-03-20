package com.dhpcs.jsonrpc.sample.models

import com.dhpcs.json.FormatBehaviors
import org.scalatest.{FunSpec, Matchers}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class AccountSpec extends FunSpec with FormatBehaviors[Account] with Matchers {

  describe("A JsValue of the wrong type") {
    it should behave like readError(
      Json.parse( """0"""),
      JsError(List(
        (__ \ "id", List(ValidationError("error.path.missing")))
      ))
    )
  }

  describe("An Account") {
    describe("without a name or metadata") {
      implicit val account = Account(
        0
      )
      implicit val accountJson = Json.parse( """{"id":0}""")
      it should behave like read
      it should behave like write
    }
    describe("with a name and metadata") {
      implicit val account = Account(
        0,
        Some("Dave's account"),
        Some(
          Json.obj(
            "hidden" -> true
          )
        )
      )
      implicit val accountJson = Json.parse( """{"id":0,"name":"Dave's account","metadata":{"hidden":true}}""")
      it should behave like read
      it should behave like write
    }
  }

}
