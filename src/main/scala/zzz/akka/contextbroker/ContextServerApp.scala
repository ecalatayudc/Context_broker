package zzz.akka.contextbroker

import akka.actor.typed.ActorSystem
import zzz.akka.contextbroker.ContextSupervisor.SayHello

object ContextServerApp extends App{
  val system: ActorSystem[ContextBroker.Message] =
    ActorSystem(ContextBroker("127.0.0.1", 8080), "BuildJobsServer")
}
