package zzz.akka.contextbroker.producer

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ContextProducerAttribute {
  def apply(): Behavior[ContextProducerMain.ValueRequestMsg] =

    Behaviors.receiveMessage {
      case ContextProducerMain.ValueRequestMsg(text, from) =>
        //#create-actors
        from ! ContextProducerMain.ValueResponseMsg("hi")
        Behaviors.empty
    }
}
