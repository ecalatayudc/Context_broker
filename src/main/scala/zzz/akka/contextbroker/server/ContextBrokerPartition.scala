package zzz.akka.contextbroker.server

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ContextBrokerPartition {
  def apply(partName: String): Behavior[List[String]] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case msg =>
        println(msg,partName)
        Behaviors.same
    }
  }
}
