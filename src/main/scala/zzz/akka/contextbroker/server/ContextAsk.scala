package zzz.akka.contextbroker.server

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import zzz.akka.contextbroker.server.ContextBrokerAttribute.offsetMsg

object ContextAsk {
  def apply(): Behavior[Any] =
    Behaviors.receiveMessage {
      case msg =>
        //println(msg,offset)
        Behaviors.same
    }
}
