package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import jdk.nashorn.internal.runtime.linker.LinkerCallSite
import zzz.akka.contextbroker.producer.ContextProducerMain.ValueAttribute
import zzz.akka.contextbroker.producer.ContextProducerPartition.{receiveMsg, spawnAttributes}
import zzz.akka.contextbroker.server.ContextBrokerEntity.{attTargetConsumers, msgPartition, partitionMsg, spawnPartition}
import zzz.akka.contextbroker.server.ContextSupervisor.StreamMsg

object ContextBrokerAttribute {
  final case class offsetMsg(msg: List[String], offset: Int)
  def apply(att: String, nPart: Int, n:Int, offset: Int, partitions: Map[Int, ActorRef[offsetMsg]]=Map.empty,consumerPartitions: Map[String,Map[String, List[Int]]]= Map.empty): Behavior[partitionMsg] = Behaviors.setup { context =>

    Behaviors.receiveMessage {
      case spawnPartition() =>
        val mapPartitions = spawnPartitions(context,nPart,partitions)
        ContextBrokerAttribute(att,nPart,0,0,partitions.++(mapPartitions),consumerPartitions)
      case msgPartition(msg) =>
        val next = nextPartition(n,partitions)
        val ref = partitions.get(next).head
        val offsetVal = getOffset(offset,50)
        ref ! offsetMsg(msg, offsetVal)
        ContextBrokerAttribute(att,nPart,next,offsetVal,partitions,consumerPartitions)
      case attTargetConsumers(idGroup,idConsumer) =>
        println(consumerPartitions)
        if (consumerPartitions.contains(idGroup)){
          val map = assignPart(idGroup,idConsumer,nPart,consumerPartitions)
          ContextBrokerAttribute(att,nPart,n,offset,partitions,consumerPartitions.+(idGroup->map))
        }else{
          val map = assignPart(idGroup,idConsumer,nPart,consumerPartitions)
          ContextBrokerAttribute(att,nPart,n,offset,partitions,consumerPartitions.updated(idGroup,map))
        }
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
  private def assignPart(idGroup:String,idConsumer:String,nPart:Int,consumerPartitions: Map[String,Map[String, List[Int]]])={
    if(consumerPartitions.contains(idGroup)){
      //sacamos el map del consumer
      val mapIdConsumer = consumerPartitions.get(idGroup).head
      //comprobamos que exista un consumer en el map
      if (mapIdConsumer.contains(idConsumer)){
        consumerPartitions.get(idGroup).head
      }else{
        val nActors = consumerPartitions.get(idGroup).head.size + 1
        val keys = consumerPartitions.get(idGroup).head.keys.toList
        if (nActors <= nPart){
          val listPart = getListPart(nPart, nActors)
          val newConsumer = Map(idConsumer->listPart.head)
          val mapUpdated = changeValues(listPart.tail,keys).reduce(_++_)
          val mapConcat = mapUpdated::newConsumer::Nil
          mapConcat.reduce(_++_)
        }else{
          val listConsumer = getListPart(nActors, nPart)
          val newKeys = keys.appended(idConsumer)
          val map = partToActors(newKeys,listConsumer,nPart)
          map.reduce(_++_)
        }
      }
    }else{
      Map(idConsumer->(1 to nPart).toList)
    }
  }
  private def getListPart(nPart:Int,nActors:Int): List[List[Int]] = nActors match {
    case _ if nActors > 0 =>
      val mod = nPart%nActors
      val nPartNotShared = (nPart-mod)/nActors
      val listNumPart =  ((nPart-nPartNotShared+1) to nPart).toList.reverse
      val resPart = nPart - nPartNotShared
      val resActor = nActors - 1
      listNumPart::getListPart(resPart,resActor)
    case _=> Nil
  }
  private def changeValues(listPart:List[List[Int]], keys: List[String]):List[Map[String, List[Int]]] = keys.length match {
    case _ if keys.length > 0 =>
      val mapConsumer = Map(keys.head->listPart.head)
      mapConsumer::changeValues(listPart.tail,keys.tail)
    case _ => Nil
  }
  private def partToActors(newKeys:List[String], listActorAssign: List[List[Int]],nPart: Int): List[Map[String, List[Int]]] = newKeys.length match {
    case _ if newKeys.length>0 =>//& newKeys.length==listActorAssign.reduce(_++_).length =>
      val key = newKeys.head
      if(listActorAssign.head.length==1){
        val nextList = listActorAssign.tail
        val part = nPart - 1
        Map(key->List(nPart))::partToActors(newKeys.tail,nextList,part)
      }else{
        val nextList = listActorAssign.head.tail::listActorAssign.tail
        Map(key->List(nPart))::partToActors(newKeys.tail,nextList,nPart)
      }
    case _ => Nil
  }
}
