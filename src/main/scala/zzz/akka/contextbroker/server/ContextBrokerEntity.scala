package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import sun.security.util.Length
import zzz.akka.contextbroker.producer.ContextProducerAttribute
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}
import zzz.akka.contextbroker.producer.ContextProducerPartition.spawnAttributes
import zzz.akka.contextbroker.server.ContextSupervisor.{ContextMsg, StreamMsg, patternSlash}

import scala.concurrent.Future
import scala.util.Random

object ContextBrokerEntity {

  trait partitionMsg
  case class spawnPartition() extends partitionMsg
  case class msgPartition(listVal: List[String]) extends partitionMsg

  def apply(listAtt:List[String], nPart: Int,attributes: Map[String, ActorRef[partitionMsg]]= Map.empty): Behavior[StreamMsg] = Behaviors.setup{ ctx=>

    val mapAtt = spawnAttributes(listAtt,ctx,nPart,attributes)
    (0 to listAtt.length-1).foreach{ n =>
      val ref = mapAtt.get(listAtt(n)).head
      ref ! spawnPartition()
    }
    Behaviors.receiveMessage {
      case StreamMsg(mapValues) =>
        (0 to listAtt.length-1).foreach{ n =>
           val ref = mapAtt.get(mapValues(n)(0)).head
           val msg = mapValues(n).drop(1)
           ref ! msgPartition(msg)
        }
        Behaviors.same
    }

  }
  private def spawnAttributes(listAtt:List[String], ctx: ActorContext[StreamMsg], nPart: Int, attributes: Map[String, ActorRef[partitionMsg]]):Map[String,ActorRef[partitionMsg]] = listAtt.length match {
    case _ if listAtt.length > 0 =>
      val next = listAtt.drop(1)
      val refAtt = ctx.spawn(ContextBrokerAttribute(listAtt(0),nPart,0,0), s"${listAtt.length}")
      val map = attributes.+(listAtt(0)->refAtt)
      spawnAttributes(next,ctx,nPart,map)
    case _ => attributes
  }
}
