package actors

import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._

sealed trait Message

case class Join(sender: String) extends Message

case class Leave(sender: String) extends Message

case class Broadcast(sender: String, content: String) extends Message

case class Chat(sender: String, content: String, status: String)

object JsonConverter {

  implicit val MessageReads: Reads[Chat] = (
    (JsPath \ "sender").read[String] and
      (JsPath \ "content").read[String] and
      (JsPath \ "status").read[String]
    ) (Chat)

  implicit val MessageWrites: Writes[Chat] = (
    (JsPath \ "sender").write[String] and
      (JsPath \ "content").write[String] and
      (JsPath \ "status").write[String]
    ) (unlift(Chat.unapply))

}