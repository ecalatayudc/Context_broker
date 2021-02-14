package zzz.akka.investigation

import akka.actor.typed.ActorSystem
import zzz.akka.investigation.ContextSupervisor.SayHello

object ContextApp extends App{
  //#actor-system
  val greeterMain: ActorSystem[ContextSupervisor.SayHello] = ActorSystem(ContextSupervisor(), "AkkaQuickStart")
  //#actor-system

  //#main-send-messages
  greeterMain ! SayHello("Charles")
  //#main-send-messages
}
