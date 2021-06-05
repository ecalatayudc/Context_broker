package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import zzz.akka.contextbroker.producer.ContextProducerMain.ValueAttribute
import zzz.akka.contextbroker.producer.ContextProducerPartition.{receiveMsg, spawnAttributes}
import zzz.akka.contextbroker.server.ContextBrokerEntity.{msgPartition, partitionMsg, spawnPartition}
import zzz.akka.contextbroker.server.ContextSupervisor.StreamMsg

object ContextBrokerAttribute {
  def apply(att: String, nPart: Int, n:Int,partitions: Map[Int, ActorRef[List[String]]]=Map.empty): Behavior[partitionMsg] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case spawnPartition() =>
        val mapPartitions = spawnPartitions(context,nPart,partitions)
        ContextBrokerAttribute(att,nPart,0,partitions.++(mapPartitions))
      case msgPartition(msg) =>
        val next = nextPartition(n,partitions)
        println(next,partitions)
        ContextBrokerAttribute(att,nPart,next,partitions)
    }
  }
  private def spawnPartitions(ctx: ActorContext[partitionMsg], nPart: Int, partitions: Map[Int, ActorRef[List[String]]]):Map[Int,ActorRef[List[String]]] = nPart match {
    case _ if nPart > 0 =>
      val next = nPart -1
      val partName = s"partition$nPart"
      val refAtt = ctx.spawn(ContextBrokerPartition(partName), partName)
      val map = partitions.+(nPart->refAtt)
      spawnPartitions(ctx,next,map)
    case _ => partitions
  }
  private def nextPartition(n:Int, map: Map[Int, ActorRef[List[String]]]):Int= n match {
    case _ if n==map.size =>  1
    case _ =>  n+1
  }
}
