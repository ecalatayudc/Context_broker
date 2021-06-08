package zzz.akka.contextbroker.server

import akka.actor.TypedActor.context
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.util.Random


object ContextSupervisor {
  // expresion regular
  val patternAttrs = "[a-zA-Z0-9]/attrs/[a-zA-Z0-9]".r
  val patternSlash = "[a-zA-Z0-9]/".r


  final case class SayHello(name: String)
  trait Info
  final case class StreamMsg (values: List[List[String]]) extends Info
  final case class InfoSubscriptionMsg (idConsumer: String, attCon: Any, url: Any, attTarget: Any, expires: String, throttling: String) extends Info
  // Definition of the a build entity and its possible status values
  sealed trait Status
  object Successful extends Status
  object Failed extends Status
  // mensajes Json
  trait JsonMsg
  final case class ContextMsg(id: String, entityType: String, attrs: String) extends JsonMsg
  final case class ContextSubscription(idGroup:String,idConsumer: String, description: String,subject:String, notification: String, expires: String, throttling: String) extends JsonMsg

  // Trait defining successful and failure responses
  sealed trait Response
  case object OK extends Response
  final case class KO(reason: String) extends Response

  // Trait and its implementations representing all possible messages that can be sent to this Behavior
  sealed trait Command
  final case class AddEntity(entity: ContextMsg, replyTo: ActorRef[Response]) extends Command
  final case class UpdateEntity(entity: ContextMsg, replyTo: ActorRef[Response]) extends Command
  final case class GetEntityById(id: String, replyTo: ActorRef[Either[Option[ContextMsg],String]]) extends Command
  final case class ClearEntity(replyTo: ActorRef[Response]) extends Command
  final case class AddSubscription(subscription: ContextSubscription, replyTo: ActorRef[Response]) extends Command
  final case class UpdateSubscription(subscription: ContextSubscription, replyTo: ActorRef[Response]) extends Command
  final case class CheckNewValues(idGroup: String,replyTo: ActorRef[Boolean]) extends Command
  // This behavior handles all possible incoming messages and keeps the state in the function parameter
  def apply(nPart: Int, entities: Map[Any, ContextMsg] = Map.empty, entitiesRef: Map[Any, ActorRef[Info]] = Map.empty, subscriptions: Map[Any, ContextSubscription] = Map.empty, newMsg:Boolean = false,valuesAttCon:Map[String, Map[Any, Serializable]] = Map.empty): Behavior[Command] = Behaviors.setup{ ctx =>
    val num = new Random().between(0.0,100.0)
    Behaviors.receiveMessage {
      case CheckNewValues(idGroup,replyTo) =>
        if(newMsg){
          if(valuesAttCon.contains(idGroup)){
            val x = compareAtt(idGroup,entities,subscriptions)
            if(valuesAttCon.get(idGroup).head ==x){
              replyTo ! false
              ContextSupervisor(nPart,entities,entitiesRef,subscriptions,false,valuesAttCon)
            }else{
              replyTo ! true
              ContextSupervisor(nPart,entities,entitiesRef,subscriptions,false,valuesAttCon.updated(idGroup,x))
            }

          }else{
            val x = compareAtt(idGroup,entities,subscriptions)
            replyTo ! true
            ContextSupervisor(nPart,entities,entitiesRef,subscriptions,false,valuesAttCon.+(idGroup->x))
          }
        }else{
          replyTo ! false
          ContextSupervisor(nPart,entities,entitiesRef,subscriptions,false,valuesAttCon)
        }
      case AddEntity(entity, replyTo) if entities.contains(entity.id) =>
        replyTo ! KO("Entity already exists")
        Behaviors.same
      case AddEntity(entity, replyTo) =>
        //      separacion de los atributos en una lista
        val mapValues = listTuple(entity)
        println(mapValues)
        val streamEntity = ctx.spawn(ContextBrokerEntity(mapValues.map(_.head),nPart,entity.id),s"context-analysis-${num}")
        streamEntity ! StreamMsg(mapValues)
        replyTo ! OK
        ContextSupervisor(nPart,entities.+(entity.id -> entity),entitiesRef.+(entity.id->streamEntity),subscriptions,true,valuesAttCon)
      case UpdateEntity(entity, replyTo) if !entities.contains(entity.id) =>
        replyTo ! KO("Entity doesn't exist")
        Behaviors.same
      case UpdateEntity(entity, replyTo) if entities.contains(entity.id) =>
        replyTo ! OK
        val mapValues = listTuple(entity)
        val ref = entitiesRef.get(entity.id).head
        ref ! StreamMsg(mapValues)
        ContextSupervisor(nPart,entities.updated(entity.id,entity),entitiesRef,subscriptions,true,valuesAttCon)
      case GetEntityById(id, replyTo) =>
        patternAttrs.findFirstMatchIn(id) match {
          case Some(_) =>
            val attr = id.split("/").toList.last
            val idPath = id.split("/").toList.head
            val listAttr = entities.get(idPath).head.attrs.split(" ").toList
            val valAttr = findAttr(attr,listAttr)
            println(valAttr)
            replyTo ! Right(valAttr)
            Behaviors.same
          case None => patternSlash.findFirstMatchIn(id) match {
            case Some(_) =>
              replyTo ! Left(entities.get(id.reverse.drop(1).reverse))
              Behaviors.same
            case None =>
              replyTo ! Left(entities.get(id))
              Behaviors.same
          }
        }
      case ClearEntity(replyTo) =>
        replyTo ! OK
        ContextSupervisor(nPart,Map.empty,Map.empty,Map.empty,false,Map.empty)
      case AddSubscription(subscription, replyTo) if subscriptions.contains(subscription.idGroup) =>
        replyTo ! KO("Entity already exists")
        Behaviors.same
      case AddSubscription(subscription, replyTo) =>
        val info = getInfoSub(subscription)
        val idEntity = info.head.head
        val attCon = info.tail.head
        val url = info.tail.tail.head.head
        val attTar = info.tail.tail.tail.head
        if (!entitiesRef.get(idEntity).isEmpty){
          replyTo ! OK
          val ref = entitiesRef.get(idEntity).head
          ref ! InfoSubscriptionMsg(subscription.idConsumer,attCon,url,attTar,subscription.expires,subscription.throttling)
        }else {
          println("pillado")
          replyTo ! KO("Entity doesn't exist")
        }
        ContextSupervisor(nPart,entities,entitiesRef,subscriptions.+(subscription.idGroup->subscription),newMsg,valuesAttCon)
      case UpdateSubscription(subscription, replyTo) if !subscriptions.contains(subscription.idGroup) =>
        replyTo ! KO("Entity doesn't exist")
        Behaviors.same
      case UpdateSubscription(subscription, replyTo) if subscriptions.contains(subscription.idGroup) =>
        val info = getInfoSub(subscription)
        val idEntity = info.head.head
        val attCon = info.tail.head
        val url = info.tail.tail.head.head
        val attTar = info.tail.tail.tail.head
        if (!entitiesRef.get(idEntity).isEmpty){
          replyTo ! OK
          val ref = entitiesRef.get(idEntity).head
          ref ! InfoSubscriptionMsg(subscription.idConsumer,attCon,url,attTar,subscription.expires,subscription.throttling)
        }else {
          replyTo ! KO("Entity doesn't exist")
        }
        ContextSupervisor(nPart,entities,entitiesRef,subscriptions.updated(subscription.idGroup,subscription),newMsg,valuesAttCon)
    }
  }

private def findAttr (attr: String, list: List[String]):String = list match {
  case x::xs =>
    val valAttr = x.split(":",2).toList
    if (valAttr.head == attr) {
      if (valAttr.tail.head.last == ','){
        valAttr.tail.head.reverse.drop(1).reverse
      }else{
        valAttr.tail.head
      }
    }else
      findAttr(attr,xs)
  case _ => ""
}
  // metodo que coge los valores del mensaje ContextMsg y lo convierte en una lista
  private def listTuple(entity:ContextMsg):List[List[String]] =
    entity.attrs.split(" ").toList.drop(1)
      //Eliminación de la última coma de los valores
      .map(x => if (x.last == ',') x.reverse.drop(1).reverse else x)
      //Separar los atributos en diferentes listas
      .map(_.split(":", 2).toList)
      //Eliminar las {} de los valores de los atributos y convertirlo en una lista de valores de atributo
      .map(x => x.head::x.tail.head.drop(1).reverse.drop(1).reverse.split(",").toList)

  //convertir la lista de valores del atributo en una tupla (attr,val, type, metadata)
//      .map(_ match {
//        case List(a,List(b, c, d)) => (a, b, c, d)
//      })
  private def getInfoSub(subscription: ContextSubscription) = {
    val idPattern = "id:[a-zA-Z0-9]*".r
    val attrsPattern = """attrs:\[[a-zA-Z0-9]*[,[a-zA-Z0-9]*]*\]""".r
    val urlPattern = "url:[a-zA-Z]*://[a-zA-Z]*:[0-9]*[/[a-zA-Z]*]*".r
    val idEntity = idPattern.findFirstIn(subscription.subject) match {
      case Some(_) =>
        idPattern.findFirstIn(subscription.subject).head.split(":").toList.drop(1).head::Nil
//        println(idPattern.findFirstIn(subscription.subject))
      case None => Nil
    }
    val conAttrs = attrsPattern.findFirstIn(subscription.subject) match {
      case Some(_) =>
        attrsPattern.findFirstIn(subscription.subject).head.split(":").toList.drop(1)
          // [attr1,attr2] -> List(attr1,attr2)
          .map(_.drop(1).reverse.drop(1).reverse.split(",").toList)(0)
//        println(attrsPattern.findFirstIn(subscription.subject))
      case None => Nil
    }
    val url = urlPattern.findFirstMatchIn(subscription.notification) match {
      case Some(_) =>
        urlPattern.findFirstIn(subscription.notification).head.split(":",2).toList.drop(1).head::Nil
//        println(urlPattern.findFirstIn(subscription.notification))
      case None => Nil
    }
    val targetAttrs = attrsPattern.findFirstIn(subscription.notification) match {
      case Some(_) =>
        attrsPattern.findFirstIn(subscription.notification).head.split(":").toList.drop(1)
          // [attr1,attr2] -> List(attr1,attr2)
          .map(_.drop(1).reverse.drop(1).reverse.split(",").toList).head
//        println(attrsPattern.findFirstIn(subscription.notification))
      case None => Nil
    }
    idEntity::conAttrs::url::targetAttrs::Nil
  }
  private def compareAtt(idGroup:Any, entities: Map[Any, ContextMsg], subscriptions: Map[Any, ContextSubscription])= {
    val mapValues: Map[Any,List[String]] = Map.empty
    val subscription = subscriptions.get(idGroup).head
    val info = getInfoSub(subscription)
    val idEntity = info.head.head
    val attCon = info.tail.head
    val msgEntity = entities.get(idEntity).head
    val attValues = listTuple(msgEntity)
    val mapAttValues = attValues.map(x=>mapValues.+(x.head->x.tail)).reduce(_++_)
    val x = mapAttValues.contains(attCon)
    val values = attCon.map(x=>mapValues.+(x->mapAttValues.get(x).head.head.split(":").toList.tail.head)).reduce(_++_)
    values
  }
}
