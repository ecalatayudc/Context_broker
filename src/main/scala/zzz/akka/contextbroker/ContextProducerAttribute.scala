package zzz.akka.contextbroker

import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{Behaviors, Routers}


object ContextProducerAttribute {
  def apply(): Behavior[ContextProducerMain.ValueRequestMsg] =

    Behaviors.receiveMessage {
      case ContextProducerMain.ValueRequestMsg(text,from) =>
        //#create-actors
        from ! ContextProducerMain.ValueResponseMsg ("hi")
        Behaviors.empty
    }
}
