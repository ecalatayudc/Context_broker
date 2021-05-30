package zzz.akka.contextbroker.producer


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}

import scala.util.Random


object ContextProducerAttribute {
  def apply(attName: String, partition: ActorRef[ValueAttribute]): Behavior[ValueAttribute] =
    Behaviors.receiveMessage {
      case ValueRequestMsg(_, from) =>
        val num = new Random().between(0.0,40.0)
        from ! ValueResponseMsg(" " + attName + s":{val:$num,type:${num.getClass},metadata:{}}",from)
        Behaviors.same
    }
}
