package zzz.akka.contextbroker.producer


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}


object ContextProducerAttribute {
  def apply(attName: String, partition: ActorRef[ValueAttribute]): Behavior[ValueAttribute] =
    Behaviors.receiveMessage {
      case ValueRequestMsg(_, from) =>
        from ! ValueResponseMsg(attName,from)
        Behaviors.same
    }
}
