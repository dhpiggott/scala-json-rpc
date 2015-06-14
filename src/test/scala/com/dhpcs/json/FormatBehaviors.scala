/*
 * Copyright (C) 2015 David Piggott <https://www.dhpcs.com>
 */
package com.dhpcs.json

import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json._

trait FormatBehaviors[A] {
  this: FunSpecLike with Matchers =>

  def decodeError[B <: A](json: JsValue, jsError: JsError)(implicit format: Format[B]) =
    it(s"$json should fail to decode with error $jsError") {
      (Json.fromJson(json)(format) should equal(jsError))(after being ordered[B])
    }

  def decode(implicit json: JsValue, a: A, format: Format[A]) =
    it(s"$json should decode to $a") {
      Json.fromJson(json)(format) should be(JsSuccess(a))
    }

  def encode(implicit a: A, json: JsValue, format: Format[A]) =
    it(s"$a should encode to $json") {
      Json.toJson(a)(format) should be(json)
    }

  def ordered[B] = new JsResultUniformity[B]

}
