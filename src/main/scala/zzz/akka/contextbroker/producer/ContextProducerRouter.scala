package zzz.akka.contextbroker.producer


import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import zzz.akka.contextbroker.producer.ContextProducerMain.{AggregatedQuotes, ValueAttribute, ValueRequestMsg, ValueResponseMsg}
import scala.concurrent.duration.DurationInt



object ContextProducerRouter {

  def apply(np: Int, att: List[String], mainProducer: ActorRef[ValueAttribute]): Behavior[ValueAttribute] = Behaviors.setup { ctx =>

    val partitions = spawnPartitions(att,np,ctx)
    Behaviors.receiveMessage {
      case ValueRequestMsg(_, from) =>
        spawnAggregator(ctx,partitions,from)
        Behaviors.same
      case AggregatedQuotes(quotes,_) =>
        ctx.log.info("{}",conMsg(quotes))
        mainProducer ! ValueResponseMsg(conMsg(quotes),mainProducer)
        Behaviors.same
      case _ => Behaviors.same
    }
  }

  private def spawnPartitions(att: List[String], npartitions: Int, ref: ActorContext[ValueAttribute]):List[ActorRef[ValueAttribute]] =
    npartitions match {
      case _ if npartitions > 0 =>
        val div = (att.length - att.length % npartitions) / npartitions
        val attHead = att.take(div)
        val attTail = att.drop(div)
        ref.spawn(ContextProducerPartition(attHead, ref.self), s"partition_$npartitions")::spawnPartitions(attTail,npartitions-1,ref)
      case _=> Nil
  }

  private def conMsg (x: List[ValueAttribute]):String = x match {
    case s::rest => s match {
      case ValueResponseMsg(text,_) =>
        text + conMsg(rest)
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
