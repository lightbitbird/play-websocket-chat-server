package controllers

import java.net.URL

import akka.event.LoggingAdapter
import play.api.mvc.RequestHeader

trait SameOriginCheck {

  protected val logging: LoggingAdapter

  def sameOriginCheck(rh: RequestHeader): Boolean = {
    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logging.debug(s"originCheck: originValue = $originValue")
        true
      case Some(badOrigin) =>
        logging.debug(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false
      case None =>
        logging.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

  def originMatches(origin: String): Boolean = {
    val url = Option(new URL(origin))
    url match {
      case Some(u) => {
        u.getHost == "localhost" && (u.getPort == 9000 || u.getPort == 19001)
        true
      }
      case None => false
    }
  }

}
