package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import play.api.libs.json._

class BroadcastActor(out: ActorRef, user: Option[String]) extends Actor with ActorLogging {

  import JsonConverter._

  override def receive = {
    case Join(s) =>
      out ! Json.toJson(Chat(s, "", "JOIN"))
    case Broadcast(s, c) =>
      out ! Json.toJson(Chat(s, c, "CHAT"))
    case Leave(s) =>
      out ! Json.toJson(Chat(s, "", "LEAVE"))
      user match {
        case Some(u) =>
          if (user == s) {
            out ! PoisonPill
            self ! PoisonPill
          }
        case None => self ! PoisonPill
      }
  }
}

object BroadcastActor {
  def props(out: ActorRef, user: Option[String]): Props = Props(classOf[BroadcastActor], out, user)
}
