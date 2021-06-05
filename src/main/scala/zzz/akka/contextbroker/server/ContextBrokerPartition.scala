package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import zzz.akka.contextbroker.server.ContextBrokerAttribute.offsetMsg

object ContextBrokerPartition {
  def apply(partName: String, mapBucket: Map[Int, ActorRef[offsetMsg]]=Map.empty): Behavior[offsetMsg] = Behaviors.setup { context =>
    Behaviors.receiveMessage {
      case offsetMsg(msg,offset) =>
        val actorBucket = context.spawn(ContextBrokerBucket(offset),s"actor-bucket-$offset")
        actorBucket ! offsetMsg(msg,offset)
        ContextBrokerPartition(partName,mapBucket.+(offset->actorBucket))
    }
  }
}
