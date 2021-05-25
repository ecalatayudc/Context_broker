package zzz.akka.contextbroker.producer


import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}
import zzz.akka.contextbroker.server.ContextServerApp.system.log


object ContextProducerAttribute {
  def apply(): Behavior[ValueAttribute] = Behaviors.setup {ctx =>
    Behaviors.receiveMessage {
      case ValueRequestMsg(text, from) =>
        val valAtt = "msg_attribute" + ctx.self.toString
        ctx.log.info(valAtt)
        from.head ! ValueResponseMsg(valAtt,from.last)
        Behaviors.empty
    }
  }
}
