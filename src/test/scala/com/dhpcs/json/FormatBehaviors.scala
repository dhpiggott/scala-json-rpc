/*
 * Copyright (C) 2015 David Piggott <https://www.dhpcs.com>
 */
package com.dhpcs.json

import org.scalatest.{FunSpecLike, Matchers}
import play.api.libs.json._

trait FormatBehaviors[A] {
  this: FunSpecLike with Matchers =>

  def readError[B <: A](json: JsValue, jsError: JsError)(implicit format: Format[B]) =
    it(s"should fail to decode with error $jsError") {
      (Json.fromJson(json)(format) should equal(jsError)) (after being ordered[B])
    }

  def read(implicit json: JsValue, a: A, format: Format[A]) =
    it(s"should decode to $a") {
      Json.fromJson(json)(format) should be(JsSuccess(a))
    }

  def write(implicit a: A, json: JsValue, format: Format[A]) =
    it(s"should encode to $json") {
      Json.toJson(a)(format) should be(json)
    }

  def ordered[B] = new JsResultUniformity[B]

}
