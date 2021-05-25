package zzz.akka.contextbroker.producer

import akka.actor.typed.ActorSystem

object ContextProducerApp extends App {
  val system: ActorSystem[ContextProducerMain.ValueResponseMsg] =
    ActorSystem(ContextProducerMain(npartitions = 2, nattributes = 2), "BuildJobsServer")
}
