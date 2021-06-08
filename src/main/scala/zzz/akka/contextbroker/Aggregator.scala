package zzz.akka.contextbroker

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.collection.immutable
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object Aggregator {

  sealed trait Command

  private case object ReceiveTimeout extends Command

  private case class WrappedReply[R](reply: R) extends Command

  def apply[Reply: ClassTag, Aggregate](
                                         sendRequests: ActorRef[Reply] => Unit,
                                         expectedReplies: Int,
                                         replyTo: ActorRef[Aggregate],
                                         aggregateReplies: immutable.IndexedSeq[Reply] => Aggregate,
                                         timeout: FiniteDuration): Behavior[Command] = {
    Behaviors.setup { context =>
      //si no se recive un mensaje manda una notificacion
      context.setReceiveTimeout(timeout, ReceiveTimeout)
      // adaptador de mensajes que convertirá o envolverá los mensajes
      // de manera que los protocolos de otros actores puedan ser ingeridos por este actor
      val replyAdapter = context.messageAdapter[Reply](WrappedReply(_))
      //especifica la referencia a la que mandaran las peticiones los actores particion o ruter
      sendRequests(replyAdapter)

      def collecting(replies: immutable.IndexedSeq[Reply]): Behavior[Command] = {
        Behaviors.receiveMessage {
          case WrappedReply(reply) =>
            // se agregan las respuestas que llegan
            val newReplies = replies :+ reply.asInstanceOf[Reply]
            // si el tamaño de la lista es igual al especificado
            // se manda el resultado y se cierra el actor
            if (newReplies.size == expectedReplies) {
              val result = aggregateReplies(newReplies)
              replyTo ! result
              Behaviors.stopped
            } else
              collecting(newReplies)
          // en caso de que salte el timeout se manda la respuesta con los mensajes recolectados
          case ReceiveTimeout =>
            val aggregate = aggregateReplies(replies)
            replyTo ! aggregate
            Behaviors.stopped
        }
      }

      collecting(Vector.empty)
    }
  }

}
