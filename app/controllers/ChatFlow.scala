package controllers

import actors.Message
import akka.NotUsed
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import play.api.libs.json.JsValue

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

trait ChatFlow {

  protected val killSwitchFlow: Flow[Message, Message, UniqueKillSwitch]

  protected def createFlow(implicit materializer: Materializer, ec: ExecutionContext) = {
    val (hubSink: Sink[Message, NotUsed], hubSource: Source[Message, NotUsed]) =
      MergeHub.source[Message](perProducerBufferSize = 16)
        .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
        .run()

    val killSwitchFlow: Flow[Message, Message, UniqueKillSwitch] = {
      Flow.fromSinkAndSource(hubSink, hubSource)
        .joinMat((KillSwitches.singleBidi[Message, Message]))(Keep.right)
        .backpressureTimeout(3.seconds)
    }
    killSwitchFlow
  }

  protected def chatFlow(source: Flow[JsValue, Message, _], sink: Flow[Message, JsValue, _])
                        (implicit materializer: Materializer, ec: ExecutionContext) = {
    val flow = source.viaMat(killSwitchFlow)(Keep.right).viaMat(sink)(Keep.right)
    Future.successful(flow)
  }

}
