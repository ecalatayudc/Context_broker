package zzz.akka.investigation

import akka.NotUsed
import akka.actor.typed.{ActorSystem, Behavior, PostStop, Signal, Terminated}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors

object ContextSupervisor {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      //#create-actors
      val contextProducer = context.spawn(ContextProducer(), "producer")
      //#create-actors

      Behaviors.receiveMessage { message =>
        //#create-actors
        val replyTo = context.spawn(ContextConsumer(max = 3), message.name)
        //#create-actors
        contextProducer ! ContextProducer.Greet(message.name, replyTo)
        Behaviors.same
      }
    }
}

