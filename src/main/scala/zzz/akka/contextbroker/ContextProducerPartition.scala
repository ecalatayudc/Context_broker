package zzz.akka.contextbroker


import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import zzz.akka.contextbroker.ContextProducerMain.ValueRequestMsg

object ContextProducerPartition {
  sealed trait Command
  case class DoLog(text: String, from: ActorRef[ContextProducerMain.ValueResponseMsg]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup[Command] { context =>
    context.log.info("Starting worker")

    Behaviors.receiveMessage {
      case DoLog(text,from) =>
        val attributeactor = context.spawn(ContextProducerAttribute(), "pool-with-broadcast")
        context.log.info("Got message {}", text)
        attributeactor ! ValueRequestMsg(text,from)
        Behaviors.same
    }
  }
}
