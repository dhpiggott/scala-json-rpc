package com.dhpcs.jsonrpc.example.models

import com.dhpcs.jsonrpc.Message.MessageFormats
import com.dhpcs.jsonrpc._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

sealed trait Message

sealed trait Command                              extends Message
case class UpdateAccountCommand(account: Account) extends Command
case class AddTransactionCommand(from: Int,
                                 to: Int,
                                 value: BigDecimal,
                                 description: Option[String] = None,
                                 metadata: Option[JsObject] = None)
    extends Command {
  require(value >= 0)
}

object AddTransactionCommand {
  implicit val AddTransactionCommandFormat: Format[AddTransactionCommand] = (
    (JsPath \ "from").format[Int] and
      (JsPath \ "to").format[Int] and
      (JsPath \ "value").format(min[BigDecimal](0)) and
      (JsPath \ "description").formatNullable[String] and
      (JsPath \ "metadata").formatNullable[JsObject]
  )(
    (from, to, value, description, metadata) =>
      AddTransactionCommand(
        from,
        to,
        value,
        description,
        metadata
    ),
    addTransactionCommand =>
      (addTransactionCommand.from,
       addTransactionCommand.to,
       addTransactionCommand.value,
       addTransactionCommand.description,
       addTransactionCommand.metadata)
  )
}

object Command extends CommandCompanion[Command] {
  override val CommandFormats = MessageFormats(
    "updateAccount"  -> Json.format[UpdateAccountCommand],
    "addTransaction" -> Json.format[AddTransactionCommand]
  )
}

sealed trait Response                            extends Message
sealed trait ResultResponse                      extends Response
case object UpdateAccountResponse                extends ResultResponse
case class AddTransactionResponse(created: Long) extends ResultResponse

object Response extends ResponseCompanion[ResultResponse] {
  override val ResponseFormats = MessageFormats(
    "updateAccount"  -> Message.objectFormat(UpdateAccountResponse),
    "addTransaction" -> Json.format[AddTransactionResponse]
  )
}

sealed trait Notification                                         extends Message
case class AccountUpdatedNotification(account: Account)           extends Notification
case class TransactionAddedNotification(transaction: Transaction) extends Notification

object Notification extends NotificationCompanion[Notification] {
  override val NotificationFormats = MessageFormats(
    "accountUpdated"   -> Json.format[AccountUpdatedNotification],
    "transactionAdded" -> Json.format[TransactionAddedNotification]
  )
}
