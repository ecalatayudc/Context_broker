package zzz.akka.investigation

import akka.actor.typed.ActorSystem
import zzz.akka.investigation.ContextSupervisor.SayHello

object ContextApp extends App{
  val system: ActorSystem[ContextBroker.Message] =
    ActorSystem(ContextBroker("127.0.0.1", 8080), "BuildJobsServer")
}
