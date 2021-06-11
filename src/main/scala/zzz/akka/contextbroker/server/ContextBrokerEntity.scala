package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import sun.security.util.Length
import zzz.akka.contextbroker.producer.ContextProducerAttribute
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}
import zzz.akka.contextbroker.producer.ContextProducerPartition.spawnAttributes
import zzz.akka.contextbroker.server.ContextSupervisor.{ContextMsg, Info, InfoSubscriptionMsg, StreamMsg, patternSlash}

import scala.concurrent.Future
import scala.util.Random

object ContextBrokerEntity {

  trait partitionMsg
  case class spawnPartition() extends partitionMsg
  case class msgPartition(listVal: List[String]) extends partitionMsg
  case class attTargetConsumers(idGroup:String,idConsumer:String) extends partitionMsg

  def apply(listAtt:List[String], nPart: Int, nameEntity:String, attributes: Map[String, ActorRef[partitionMsg]]= Map.empty,consumers: Map[String, List[Any]]= Map.empty): Behavior[Info] = Behaviors.setup{ ctx=>

    val mapAtt = spawnAttributes(listAtt,ctx,nPart,attributes)
    (0 to listAtt.length-1).foreach{ n =>
      val ref = mapAtt.get(listAtt(n)).head
      ref ! spawnPartition()
    }
    Behaviors.receiveMessage {
      case StreamMsg(mapValues) =>
        (0 to listAtt.length-1).foreach{ n =>
           val ref = mapAtt.get(mapValues(n).head).head
           val msg = mapValues(n).drop(1)
           ref ! msgPartition(msg)
        }
        Behaviors.same
      case InfoSubscriptionMsg(idGroup,idConsumer, attCon, url, attTarget, expires, throttling) =>
        (0 to attTarget.length-1).foreach{n=>
          val ref = mapAtt.get(attTarget(n)).head
          ref ! attTargetConsumers(idGroup,idConsumer)
        }
        Behaviors.same
        //ContextBrokerEntity(listAtt,nPart,nameEntity,attributes,consumers.+(idConsumer->listValuesConsumer))
    }

  }
  private def spawnAttributes(listAtt:List[String], ctx: ActorContext[Info], nPart: Int, attributes: Map[String, ActorRef[partitionMsg]]):Map[String,ActorRef[partitionMsg]] = listAtt.length match {
    case _ if listAtt.length > 0 =>
      val next = listAtt.drop(1)
      val refAtt = ctx.spawn(ContextBrokerAttribute(listAtt(0),nPart,0,0), s"${listAtt.length}")
      val map = attributes.+(listAtt(0)->refAtt)
      spawnAttributes(next,ctx,nPart,map)
    case _ => attributes
  }
}
