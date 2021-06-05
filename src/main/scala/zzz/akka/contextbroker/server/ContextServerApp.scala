package zzz.akka.contextbroker.server

import akka.actor.typed.ActorSystem

object ContextServerApp extends App {
  val nPart = 2
  val system: ActorSystem[ContextBroker.Message] =
    ActorSystem(ContextBroker("127.0.0.1", 5804,nPart), "BuildEntitiesServer")
}
