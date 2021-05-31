package zzz.akka.contextbroker.server

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.util.matching.Regex

object ContextSupervisor {
  // expresion regular
  val patternAttrs = "[a-zA-Z0-9]/attrs/[a-zA-Z0-9]".r
  val patternSlash = "[a-zA-Z0-9]/".r



  final case class SayHello(name: String)
  // Definition of the a build entity and its possible status values
  sealed trait Status
  object Successful extends Status
  object Failed extends Status
  final case class ContextMsg(id: String, entityType: String, attrs: String)

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


  // This behavior handles all possible incoming messages and keeps the state in the function parameter
  def apply(entities: Map[String, ContextMsg] = Map.empty): Behavior[Command] = Behaviors.receiveMessage {
    case AddEntity(entity, replyTo) if entities.contains(entity.id) =>
      replyTo ! KO("Entity already exists")
      Behaviors.same
    case UpdateEntity(entity, replyTo) if !entities.contains(entity.id) =>
      replyTo ! KO("Entity doesn't exist")
      Behaviors.same
    case UpdateEntity(entity, replyTo) if entities.contains(entity.id) =>
      replyTo ! OK
      ContextSupervisor(entities.updated(entity.id,entity))
    case AddEntity(entity, replyTo) =>
      replyTo ! OK
      ContextSupervisor(entities.+(entity.id -> entity))
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
      ContextSupervisor(Map.empty)
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
}
