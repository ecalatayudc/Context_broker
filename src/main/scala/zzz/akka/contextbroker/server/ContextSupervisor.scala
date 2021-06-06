package zzz.akka.contextbroker.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.util.Random


object ContextSupervisor {
  // expresion regular
  val patternAttrs = "[a-zA-Z0-9]/attrs/[a-zA-Z0-9]".r
  val patternSlash = "[a-zA-Z0-9]/".r


  final case class StreamMsg (values: List[List[String]])
  final case class SayHello(name: String)
  // Definition of the a build entity and its possible status values
  sealed trait Status
  object Successful extends Status
  object Failed extends Status
  // mensajes Json
  trait JsonMsg
  final case class ContextMsg(id: String, entityType: String, attrs: String) extends JsonMsg
  final case class ContextSubscription(id: String, description: String,subject:String, notification: String, expires: String, throttling: String) extends JsonMsg

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

  // This behavior handles all possible incoming messages and keeps the state in the function parameter
  def apply(nPart: Int,entities: Map[String, ContextMsg] = Map.empty, entitiesRef: Map[String, ActorRef[StreamMsg]] = Map.empty, subscriptions: Map[String, ContextSubscription] = Map.empty): Behavior[Command] = Behaviors.setup{ctx =>
    val num = new Random().between(0.0,100.0)
    Behaviors.receiveMessage {
      case AddEntity(entity, replyTo) if entities.contains(entity.id) =>
        replyTo ! KO("Entity already exists")
        Behaviors.same
      case AddEntity(entity, replyTo) =>
        //      separacion de los atributos en una lista
        val mapValues = listTuple(entity)
        val streamEntity = ctx.spawn(ContextBrokerEntity(mapValues.map(_(0)),nPart),s"context-analysis-${num}")
        streamEntity ! StreamMsg(mapValues)
        replyTo ! OK
        ContextSupervisor(nPart,entities.+(entity.id -> entity),entitiesRef.+(entity.id->streamEntity),subscriptions)
      case UpdateEntity(entity, replyTo) if !entities.contains(entity.id) =>
        replyTo ! KO("Entity doesn't exist")
        Behaviors.same
      case UpdateEntity(entity, replyTo) if entities.contains(entity.id) =>
        replyTo ! OK
        val mapValues = listTuple(entity)
        val ref = entitiesRef.get(entity.id).head
        ref ! StreamMsg(mapValues)
        ContextSupervisor(nPart,entities.updated(entity.id,entity),entitiesRef,subscriptions)
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
        ContextSupervisor(nPart,Map.empty,Map.empty)
      case AddSubscription(subscription, replyTo) if subscriptions.contains(subscription.id) =>
        replyTo ! KO("Entity already exists")
        Behaviors.same
      case AddSubscription(subscription, replyTo) =>
        replyTo ! OK
        println(subscriptions)
        ContextSupervisor(nPart,entities,entitiesRef,subscriptions.+(subscription.id->subscription))
      case UpdateSubscription(subscription, replyTo) if !subscriptions.contains(subscription.id) =>
        replyTo ! KO("Entity doesn't exist")
        Behaviors.same
      case UpdateSubscription(subscription, replyTo) if subscriptions.contains(subscription.id) =>
        replyTo ! OK
        println(subscriptions)
        ContextSupervisor(nPart,entities,entitiesRef,subscriptions.updated(subscription.id,subscription))
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
}
