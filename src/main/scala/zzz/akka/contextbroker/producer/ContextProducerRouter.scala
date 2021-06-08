package zzz.akka.contextbroker.producer


import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import zzz.akka.contextbroker.Aggregator
import zzz.akka.contextbroker.producer.ContextProducerMain.{AggregatedQuotes, ValueAttribute, ValueRequestMsg, ValueResponseMsg}

import scala.concurrent.duration.DurationInt



object ContextProducerRouter {

  def apply(np: Int, att: List[String], mainProducer: ActorRef[ValueAttribute]): Behavior[ValueAttribute] =
    Behaviors.setup { ctx =>
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
  // metodo que genera actores particion y devuleve una lista con las referencias de los actores generados
  private def spawnPartitions(att: List[String], npartitions: Int, ref: ActorContext[ValueAttribute]):List[ActorRef[ValueAttribute]] =
    npartitions match {
      case _ if npartitions > 0 =>
        val div = (att.length - att.length % npartitions) / npartitions
        val attHead = att.take(div)
        val attTail = att.drop(div)
        ref.spawn(ContextProducerPartition(attHead, ref.self), s"partition_$npartitions")::spawnPartitions(attTail,npartitions-1,ref)
      case _=> Nil
  }
  // metodo que concatena los textos de los mensajes
  private def conMsg (x: List[ValueAttribute]):String = x match {
    case s::rest => s match {
      case ValueResponseMsg(text,_) =>
        text + conMsg(rest)
    }
    case _ => ""
  }
  //metodo que genera el actor agregador y devuelve una lista con los mensajes agregados
  private def spawnAggregator(context: ActorContext[ValueAttribute], attributes: List[ActorRef[ValueAttribute]],from: ActorRef[ValueResponseMsg])=
    context.spawnAnonymous(
      //[tipo de los mensajes,tipo que devuelve]
      Aggregator[ValueAttribute, AggregatedQuotes](
        //replyto es la referencia al actor agregador
        sendRequests = { replyTo =>
          attributes.foreach { n =>
            n ! ValueRequestMsg("hi",replyTo)
          }
        },
        //numero experado de respuestas
        expectedReplies = attributes.length,
        //referencia al actor al que se le mandan los mensajes agrupados
        context.self,
        aggregateReplies = replies =>
          //especificacion de como agregar los mensajes, en este caso se
          // pasan directamente a una lista y se encapsula en el mensaje AggregatedQuotes
          AggregatedQuotes(
            replies
              .toList,from),
        timeout = 5.seconds))
}
