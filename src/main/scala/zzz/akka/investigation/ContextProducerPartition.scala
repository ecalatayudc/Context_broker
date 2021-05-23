package zzz.akka.investigation

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ContextProducerPartition {
  sealed trait Command
  case class DoLog(text: String) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    context.log.info("Starting worker")

    Behaviors.receiveMessage {
      case DoLog(text) =>
        context.log.info("Got message {}", text)
        Behaviors.same
    }
  }
}
