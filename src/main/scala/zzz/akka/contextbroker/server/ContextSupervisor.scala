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
  final case class CheckNewValues(replyTo: ActorRef[Boolean]) extends Command
  // This behavior handles all possible incoming messages and keeps the state in the function parameter
  def apply(nPart: Int,entities: Map[String, ContextMsg] = Map.empty, entitiesRef: Map[Any, ActorRef[Info]] = Map.empty, subscriptions: Map[String, ContextSubscription] = Map.empty,newValue:Boolean = false): Behavior[Command] = Behaviors.setup{ctx =>
    val num = new Random().between(0.0,100.0)
    Behaviors.receiveMessage {
      case CheckNewValues(replyTo) =>
        if (newValue){
          replyTo ! true
        }else{
          replyTo ! false
        }
        ContextSupervisor(nPart,entities,entitiesRef,subscriptions,false)
      case AddEntity(entity, replyTo) if entities.contains(entity.id) =>
        replyTo ! KO("Entity already exists")
        Behaviors.same
      case AddEntity(entity, replyTo) =>
        //      separacion de los atributos en una lista
        val mapValues = listTuple(entity)
        val streamEntity = ctx.spawn(ContextBrokerEntity(mapValues.map(_(0)),nPart,entity.id),s"context-analysis-${num}")
        streamEntity ! StreamMsg(mapValues)
        replyTo ! OK
        ContextSupervisor(nPart,entities.+(entity.id -> entity),entitiesRef.+(entity.id->streamEntity),subscriptions,true)
      case UpdateEntity(entity, replyTo) if !entities.contains(entity.id) =>
        replyTo ! KO("Entity doesn't exist")
        Behaviors.same
      case UpdateEntity(entity, replyTo) if entities.contains(entity.id) =>
        replyTo ! OK
        val mapValues = listTuple(entity)
        val ref = entitiesRef.get(entity.id).head
        ref ! StreamMsg(mapValues)
        ContextSupervisor(nPart,entities.updated(entity.id,entity),entitiesRef,subscriptions,true)
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
        ContextSupervisor(nPart,Map.empty,Map.empty,Map.empty,false)
      case AddSubscription(subscription, replyTo) if subscriptions.contains(subscription.idGroup) =>
        replyTo ! KO("Entity already exists")
        Behaviors.same
      case AddSubscription(subscription, replyTo) =>
        replyTo ! OK
        val info = getInfoSub(subscription)
        val attCon = info.tail.head
        val url = info.tail.tail.head
        val attTar = info.tail.tail.tail.head
        if (!entitiesRef.get(info.head).isEmpty){
          val ref = entitiesRef.get(info.head).head
          ref ! InfoSubscriptionMsg(subscription.idConsumer,attCon,url,attTar,subscription.expires,subscription.throttling)
        }else {
          replyTo ! KO("Entity doesn't exist")
        }
        ContextSupervisor(nPart,entities,entitiesRef,subscriptions.+(subscription.idGroup->subscription),newValue)
      case UpdateSubscription(subscription, replyTo) if !subscriptions.contains(subscription.idGroup) =>
        replyTo ! KO("Entity doesn't exist")
        Behaviors.same
      case UpdateSubscription(subscription, replyTo) if subscriptions.contains(subscription.idGroup) =>
        replyTo ! OK
        val info = getInfoSub(subscription)
        val attCon = info.tail.head
        val url = info.tail.tail.head
        val attTar = info.tail.tail.tail.head
        if (!entitiesRef.get(info.head).isEmpty){
          val ref = entitiesRef.get(info.head).head
          ref ! InfoSubscriptionMsg(subscription.idConsumer,attCon,url,attTar,subscription.expires,subscription.throttling)
        }else {
          replyTo ! KO("Entity doesn't exist")
        }
        ContextSupervisor(nPart,entities,entitiesRef,subscriptions.updated(subscription.idGroup,subscription),newValue)
    }
  }

private def findAttr (attr: String, list: List[String]):String = list match {
  case x::xs =>
    val valAttr = x.split(":",2).toList
    if (valAttr(0) == attr) {
      if (valAttr(1).last == ','){
        valAttr(1).reverse.drop(1).reverse
      }else{
        valAttr(1)
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
      .map(x => x(0)::x(1).drop(1).reverse.drop(1).reverse.split(",").toList)
      //convertir la lista de valores del atributo en una tupla (attr,val, type, metadata)
//      .map(_ match {
//        case List(a,List(b, c, d)) => (a, b, c, d)
//      })
  private def getInfoSub(subscription: ContextSubscription) = {
    val idPattern = "id:[a-zA-Z0-9]*".r
    val attrsPattern = """attrs:\[[a-zA-Z0-9]*[,[a-zA-Z0-9]*]*\]""".r
    val urlPattern = "url:[a-zA-Z]*://[a-zA-Z]*:[0-9]*[/[a-zA-Z]*]*".r
    val id = idPattern.findFirstIn(subscription.subject) match {
      case Some(_) =>
        idPattern.findFirstIn(subscription.subject).head.split(":").toList.drop(1)(0)
//        println(idPattern.findFirstIn(subscription.subject))
      case None => println("id not found")
    }
    val conAttrs = attrsPattern.findFirstIn(subscription.subject) match {
      case Some(_) =>
        attrsPattern.findFirstIn(subscription.subject).head.split(":").toList.drop(1)
          // [attr1,attr2] -> List(attr1,attr2)
          .map(_.drop(1).reverse.drop(1).reverse.split(",").toList)(0)
//        println(attrsPattern.findFirstIn(subscription.subject))
      case None => println("attrs not found in subject")
    }
    val url = urlPattern.findFirstMatchIn(subscription.notification) match {
      case Some(_) =>
        urlPattern.findFirstIn(subscription.notification).head.split(":",2).toList.drop(1)(0)
//        println(urlPattern.findFirstIn(subscription.notification))
      case None => println("url not found")
    }
    val targetAttrs = attrsPattern.findFirstIn(subscription.notification) match {
      case Some(_) =>
        attrsPattern.findFirstIn(subscription.notification).head.split(":").toList.drop(1)
          // [attr1,attr2] -> List(attr1,attr2)
          .map(_.drop(1).reverse.drop(1).reverse.split(",").toList)(0)
//        println(attrsPattern.findFirstIn(subscription.notification))
      case None => println("attrs not found in notifications")
    }
    id::conAttrs::url::targetAttrs::Nil
  }
}
