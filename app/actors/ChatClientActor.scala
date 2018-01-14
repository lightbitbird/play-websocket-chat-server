package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import play.api.libs.json.JsValue

class ChatClientActor(out: ActorRef, user: Option[String]) extends Actor with ActorLogging {

  import JsonConverter._

  override def receive = {
    case msg: JsValue => {
      val chat = msg.as[Chat]
      if (chat.status == "CHAT") {
        out ! Broadcast(chat.sender, chat.content)
      }
    }
  }

  override def preStart(): Unit = {
    user match {
      case Some(u) => out ! Join(u)
      case None =>
        log.info(s"No user name ${out.path}")
        self ! PoisonPill
    }
  }

  override def postStop(): Unit = {
    user match {
      case Some(u) => out ! Leave(u)
      case None => log.info(s"Stop ${out.path}")
    }
    self ! PoisonPill
  }
}

object ChatClientActor {
  def props(out: ActorRef, user: Option[String]): Props = Props(classOf[ChatClientActor], out, user)
}