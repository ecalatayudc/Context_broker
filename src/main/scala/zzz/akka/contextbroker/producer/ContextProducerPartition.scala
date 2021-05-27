package zzz.akka.contextbroker.producer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import zzz.akka.contextbroker.producer.ContextProducerMain.{AggregatedQuotes, ValueAttribute, ValueRequestMsg, ValueResponseMsg}
import scala.concurrent.duration.DurationInt


object ContextProducerPartition {

  def apply(att: List[String],router: ActorRef[ValueAttribute]): Behavior[ValueAttribute] = Behaviors.setup { context =>
    context.log.info("Starting worker")
    val attributes = spawnAttributes(att,context)
    receiveMsg(att.toString,context,attributes,router)
  }

  private def receiveMsg(msg: String, context: ActorContext[ValueAttribute],attributes: List[ActorRef[ValueAttribute]],router: ActorRef[ValueAttribute]): Behavior[ValueAttribute] =
    Behaviors.receiveMessage {
      case ValueRequestMsg(text, from) =>
        context.log.info("Got message {}", text)
        spawnAggregator(context,attributes,from)
        Behaviors.same
      case AggregatedQuotes(quotes,from) =>
        context.log.info("{}",conMsg(quotes))
        from ! ValueResponseMsg(conMsg(quotes),from)
        Behaviors.same
      case _ => Behaviors.same
  }
  private def spawnAttributes(att: List[String], ctx: ActorContext[ValueAttribute]):List[ActorRef[ValueAttribute]] = att.length match {
    case _ if att.length > 0 =>
      ctx.spawn(ContextProducerAttribute(att.head,ctx.self), s"${att.head}")::spawnAttributes(att.tail,ctx)
    case _ => Nil
  }

  private def conMsg (x: List[ValueAttribute]):String = x match {
    case s::rest => s match {
      case ValueResponseMsg(text,_) =>
        text + "," + conMsg(rest)
    }
    case _ => ""

  }
  private def spawnAggregator(context: ActorContext[ValueAttribute], attributes: List[ActorRef[ValueAttribute]],from: ActorRef[ValueResponseMsg])=
    context.spawnAnonymous(
    Aggregator[ValueAttribute, AggregatedQuotes](
      sendRequests = { replyTo =>
        attributes.foreach { n =>
          n ! ValueRequestMsg("hi",replyTo)
        }
      },
      expectedReplies = attributes.length,
      context.self,
      aggregateReplies = replies =>
        AggregatedQuotes(
          replies
            .toList,from),
      timeout = 5.seconds))

}
