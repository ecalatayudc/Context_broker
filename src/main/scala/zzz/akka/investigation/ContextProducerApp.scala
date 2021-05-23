package zzz.akka.investigation

import akka.actor.typed.ActorSystem

object ContextProducerApp extends App{
  val system: ActorSystem[Unit] =
    ActorSystem(ContextProducer(), "BuildJobsServer")
}
