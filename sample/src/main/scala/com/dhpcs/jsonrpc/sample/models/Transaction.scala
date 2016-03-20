package com.dhpcs.jsonrpc.sample.models

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

case class Transaction(from: Int,
                       to: Int,
                       value: BigDecimal,
                       created: Long,
                       description: Option[String] = None,
                       metadata: Option[JsObject] = None) {
  require(value >= 0)
  require(created >= 0)
}

object Transaction {

  implicit val TransactionFormat: Format[Transaction] = (
    (JsPath \ "from").format[Int] and
      (JsPath \ "to").format[Int] and
      (JsPath \ "value").format(min[BigDecimal](0)) and
      (JsPath \ "created").format(min[Long](0)) and
      (JsPath \ "description").formatNullable[String] and
      (JsPath \ "metadata").formatNullable[JsObject]
    )((from, to, value, created, description, metadata) =>
    Transaction(
      from,
      to,
      value,
      created,
      description,
      metadata
    ), transaction =>
    (transaction.from,
      transaction.to,
      transaction.value,
      transaction.created,
      transaction.description,
      transaction.metadata)
    )

}
