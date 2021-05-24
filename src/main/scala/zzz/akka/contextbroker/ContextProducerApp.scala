package zzz.akka.contextbroker

import akka.actor.typed.ActorSystem

object ContextProducerApp extends App{
  val system: ActorSystem[ContextProducerMain.ValueResponseMsg] =
    ActorSystem(ContextProducerMain(npartitions = 4, nattributes = 2), "BuildJobsServer")
}
