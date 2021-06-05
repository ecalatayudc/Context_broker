package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import zzz.akka.contextbroker.producer.ContextProducerMain.ValueAttribute
import zzz.akka.contextbroker.producer.ContextProducerPartition.{receiveMsg, spawnAttributes}
import zzz.akka.contextbroker.server.ContextBrokerEntity.{msgPartition, partitionMsg, spawnPartition}
import zzz.akka.contextbroker.server.ContextSupervisor.StreamMsg

object ContextBrokerAttribute {
  final case class offsetMsg(msg: List[String], offset: Int)
  def apply(att: String, nPart: Int, n:Int, offset: Int, partitions: Map[Int, ActorRef[offsetMsg]]=Map.empty): Behavior[partitionMsg] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case spawnPartition() =>
        val mapPartitions = spawnPartitions(context,nPart,partitions)
        ContextBrokerAttribute(att,nPart,0,0,partitions.++(mapPartitions))
      case msgPartition(msg) =>
        val next = nextPartition(n,partitions)
        val ref = partitions.get(next).head
        val offsetVal = getOffset(offset,50)
        ref ! offsetMsg(msg, offsetVal)
        ContextBrokerAttribute(att,nPart,next,offsetVal,partitions)
    }
  }
  private def spawnPartitions(ctx: ActorContext[partitionMsg], nPart: Int, partitions: Map[Int, ActorRef[offsetMsg]]):Map[Int,ActorRef[offsetMsg]] = nPart match {
    case _ if nPart > 0 =>
      val next = nPart -1
      val partName = s"partition$nPart"
      val refAtt = ctx.spawn(ContextBrokerPartition(partName), partName)
      val map = partitions.+(nPart->refAtt)
      spawnPartitions(ctx,next,map)
    case _ => partitions
  }
  private def nextPartition(n:Int, map: Map[Int, ActorRef[offsetMsg]]):Int= n match {
    case _ if n < map.size =>  n+1
    case _ =>  1
  }
  private def getOffset(n: Int, max: Int) = n match {
    case _ if n < max => n+1
    case _ => n
  }
}
