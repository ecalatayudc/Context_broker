package zzz.akka.contextbroker.producer

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{Behavior, SupervisorStrategy}
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}

object ContextProducerRouter {

  def apply(np: Int, natt: Int): Behavior[ValueRequestMsg] = Behaviors.setup { ctx =>
    val pool = Routers.pool(poolSize = np) {
      // make sure the workers are restarted if they fail
      Behaviors.supervise(ContextProducerPartition(natt)).onFailure[Exception](SupervisorStrategy.restart)
    }
    val poolWithBroadcast = pool.withBroadcastPredicate(_.isInstanceOf[DoBroadcastLog])
    val routerWithBroadcast = ctx.spawn(poolWithBroadcast, "pool-with-broadcast")
    //      val router = ctx.spawn(pool, "worker-pool")

    //      (0 to 10).foreach { n =>
    //        router ! ContextProducerPartition.DoLog(s"msg $n")
    //      }
    Behaviors.receiveMessage {
      case ValueRequestMsg(text, from) =>
        //#create-actors

        //this will be sent to all 4 routees
        routerWithBroadcast ! DoBroadcastLog(text, from)
        Behaviors.same
    }
  }
}
