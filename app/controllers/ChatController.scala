package controllers

import actors.{BroadcastActor, ChatClientActor, Message}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.Flow
import akka.stream.{Materializer, UniqueKillSwitch}
import akka.util.Timeout
import com.google.inject.Inject
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.ActorFlow
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}


case class WebsocketFlow(flow: Flow[Message, Message, UniqueKillSwitch])

class ChatController @Inject()(cc: ControllerComponents)
                              (implicit actorSystem: ActorSystem,
                               mat: Materializer,
                               executionContext: ExecutionContext)
  extends AbstractController(cc) with SameOriginCheck with ChatFlow {

  protected lazy val logging = Logging(actorSystem, getClass)
  protected lazy val killSwitchFlow: Flow[Message, Message, UniqueKillSwitch] = createFlow
  implicit val timeout = Timeout(2.seconds)

  def index: Action[AnyContent] = Action { implicit request =>
    Ok(views.html.index())
  }

  def chat: WebSocket = {
    WebSocket.acceptOrResult[JsValue, JsValue] {
      case rh if sameOriginCheck(rh) =>
        val user = rh.queryString("user").headOption
        val chatClient = ActorFlow.actorRef[JsValue, Message](out => ChatClientActor.props(out, user))
        val broadcast = ActorFlow.actorRef[Message, JsValue](out => BroadcastActor.props(out, user))

        chatFlow(chatClient, broadcast).map { flow =>
          Right(flow)
        }.recover {
          case e: Exception =>
            logging.error("Cannot create websocket", e)
            val jsError = Json.obj("error" -> "Cannot create websocket")
            val result = InternalServerError(jsError)
            Left(result)
        }

      case rejected =>
        logging.error(s"Request ${rejected} failed same origin check")
        Future.successful {
          Left(Forbidden("forbidden"))
        }
    }
  }
}
