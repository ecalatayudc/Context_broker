package zzz.akka.contextbroker.producer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import zzz.akka.contextbroker.Aggregator
import zzz.akka.contextbroker.producer.ContextProducerMain.{AggregatedQuotes, ValueAttribute, ValueRequestMsg, ValueResponseMsg}

import scala.concurrent.duration.DurationInt


object ContextProducerPartition {

  def apply(att: List[String],router: ActorRef[ValueAttribute]): Behavior[ValueAttribute] = Behaviors.setup { context =>
    context.log.info("Starting worker")
    //lista de las referencias a actores atributos
    val attributes = spawnAttributes(att,context)
    //metodo que maneja los mensajes entrantes
    receiveMsg(att.toString,context,attributes,router)
  }

  private def receiveMsg(msg: String, context: ActorContext[ValueAttribute],attributes: List[ActorRef[ValueAttribute]],router: ActorRef[ValueAttribute]): Behavior[ValueAttribute] =
    Behaviors.receiveMessage {
      case ValueRequestMsg(text, from) =>
        context.log.info("Got message {}", text)
        //metodo que genera un actor que agrupa los mensjaes de los atributos
        spawnAggregator(context,attributes,from)
        Behaviors.same
      case AggregatedQuotes(quotes,from) =>
        context.log.info("{}",conMsg(quotes))
        from ! ValueResponseMsg(conMsg(quotes),from)
        Behaviors.same
      case _ => Behaviors.same
  }
  //metodo que genera una lista de actores atributos
  private def spawnAttributes(att: List[String], ctx: ActorContext[ValueAttribute]):List[ActorRef[ValueAttribute]] = att.length match {
    case _ if att.length > 0 =>
      ctx.spawn(ContextProducerAttribute(att.head,ctx.self), s"${att.head}")::spawnAttributes(att.tail,ctx)
    case _ => Nil
  }
  // metodo que concatena los textos encerrados en los mensajes
  private def conMsg (x: List[ValueAttribute]):String = x match {
    case s::rest => s match {
      case ValueResponseMsg(text,_) =>
        text + "," + conMsg(rest)
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
      //especificacion de como agregar los mensajes, en este caso se pasan directamente a una lista y se encapsula en el mensaje AggregatedQuotes
      aggregateReplies = replies =>
        AggregatedQuotes(
          replies
            .toList,from),
      timeout = 5.seconds))

}
