package com.dhpcs.jsonrpc.sample

import com.dhpcs.jsonrpc.Message.MethodFormats
import com.dhpcs.jsonrpc._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

sealed trait Message

sealed trait Command extends Message

case class UpdateAccountCommand(account: Account) extends Command

case class AddTransactionCommand(from: Int,
                                 to: Int,
                                 value: BigDecimal,
                                 description: Option[String] = None,
                                 metadata: Option[JsObject] = None) extends Command {
  require(value >= 0)
}

object AddTransactionCommand {

  implicit val AddTransactionCommandFormat: Format[AddTransactionCommand] = (
    (JsPath \ "from").format[Int] and
      (JsPath \ "to").format[Int] and
      (JsPath \ "value").format(min[BigDecimal](0)) and
      (JsPath \ "description").formatNullable[String] and
      (JsPath \ "metadata").formatNullable[JsObject]
    )((from, to, value, description, metadata) =>
    AddTransactionCommand(
      from,
      to,
      value,
      description,
      metadata
    ), addTransactionCommand =>
    (addTransactionCommand.from,
      addTransactionCommand.to,
      addTransactionCommand.value,
      addTransactionCommand.description,
      addTransactionCommand.metadata)
    )

}

object Command extends CommandCompanion[Command] {

  override val CommandTypeFormats = MethodFormats(
    "updateAccount" -> Json.format[UpdateAccountCommand],
    "addTransaction" -> Json.format[AddTransactionCommand]
  )

}

sealed trait Response extends Message

case object UpdateAccountResponse extends Response

case class AddTransactionResponse(created: Long) extends Response

object Response extends ResponseCompanion[Response] {

  override val ResponseFormats = MethodFormats(
    "updateAccount" -> UpdateAccountResponse,
    "addTransaction" -> Json.format[AddTransactionResponse]
  )

}

sealed trait Notification extends Message

case class AccountUpdatedNotification(account: Account) extends Notification

case class TransactionAddedNotification(transaction: Transaction) extends Notification

object Notification extends NotificationCompanion[Notification] {

  override val NotificationFormats = MethodFormats(
    "accountUpdated" -> Json.format[AccountUpdatedNotification],
    "transactionAdded" -> Json.format[TransactionAddedNotification]
  )

}