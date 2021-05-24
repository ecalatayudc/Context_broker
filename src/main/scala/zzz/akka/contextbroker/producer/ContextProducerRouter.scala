package zzz.akka.contextbroker.producer

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{Behavior, SupervisorStrategy}
import zzz.akka.contextbroker.producer.ContextProducerMain.ValueRequestMsg

object ContextProducerRouter {

  def apply(np: Int, natt: Int): Behavior[ContextProducerMain.ValueRequestMsg] = Behaviors.setup[ContextProducerMain.ValueRequestMsg] { ctx =>
    val pool = Routers.pool(poolSize = np) {
      // make sure the workers are restarted if they fail
      Behaviors.supervise(ContextProducerPartition()).onFailure[Exception](SupervisorStrategy.restart)
    }
    //      val router = ctx.spawn(pool, "worker-pool")

    //      (0 to 10).foreach { n =>
    //        router ! ContextProducerPartition.DoLog(s"msg $n")
    //      }
    Behaviors.receiveMessage {
      case ValueRequestMsg(text, from) =>
        //#create-actors
        val poolWithBroadcast = pool.withBroadcastPredicate(_.isInstanceOf[DoBroadcastLog])
        val routerWithBroadcast = ctx.spawn(poolWithBroadcast, "pool-with-broadcast")
        //this will be sent to all 4 routees
        routerWithBroadcast ! DoBroadcastLog(text, from)
        Behaviors.empty
    }
  }
}
