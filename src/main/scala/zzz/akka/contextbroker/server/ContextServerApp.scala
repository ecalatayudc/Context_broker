package zzz.akka.contextbroker.server

import akka.actor.typed.ActorSystem

object ContextServerApp extends App {
  val system: ActorSystem[ContextBroker.Message] =
    ActorSystem(ContextBroker("127.0.0.1", 8080), "BuildJobsServer")
}
