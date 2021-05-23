package zzz.akka.investigation

import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{Behaviors, Routers}

object ContextProducer {
  sealed trait ValueRequest
  case class ValueRequestMsg(text: String) extends ValueRequest
  def apply(): Behavior[Unit] = Behaviors.setup[Unit] { ctx =>

    val routerWithBroadcast = ctx.spawn(ContextProducerRouter(), "pool-with-broadcast")
    //this will be sent to all 4 routees
    routerWithBroadcast ! ValueRequestMsg("msg_broadcast_producer")
    Behaviors.empty
  }
}
