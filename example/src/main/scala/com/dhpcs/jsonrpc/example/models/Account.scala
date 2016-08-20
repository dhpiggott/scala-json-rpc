package com.dhpcs.jsonrpc.example.models

import play.api.libs.json._

case class Account(id: Int,
                   name: Option[String] = None,
                   metadata: Option[JsObject] = None)

object Account {
  implicit val AccountFormat = Json.format[Account]
}
