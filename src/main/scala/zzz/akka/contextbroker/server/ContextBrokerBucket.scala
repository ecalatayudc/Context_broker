package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import zzz.akka.contextbroker.server.ContextBrokerAttribute.{nextPartition, offsetMsg, spawnPartitions}
import zzz.akka.contextbroker.server.ContextBrokerEntity.{msgPartition, partitionMsg, spawnPartition}

object ContextBrokerBucket {
  def apply(offset: Int): Behavior[offsetMsg] =
    Behaviors.receiveMessage {
      case offsetMsg(msg,_) =>
        //println(msg,offset)
        Behaviors.same
    }
}
