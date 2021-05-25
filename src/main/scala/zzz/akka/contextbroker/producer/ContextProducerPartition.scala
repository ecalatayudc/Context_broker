package zzz.akka.contextbroker.producer

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}

import scala.::
import scala.collection.immutable.Nil.:::

object ContextProducerPartition {



  def apply(natt: Int): Behavior[ValueAttribute] = Behaviors.setup { context =>
    context.log.info("Starting worker")
    val pool = Routers.pool(poolSize = natt) {
      // make sure the workers are restarted if they fail
      Behaviors.supervise(ContextProducerAttribute()).onFailure[Exception](SupervisorStrategy.restart)
    }
    val poolAttribute = pool.withBroadcastPredicate(_.isInstanceOf[DoBroadcastLog])
    val routerWithBroadcast = context.spawn(poolAttribute, "pool-with-broadcast")
    Behaviors.receiveMessage {
      case ValueRequestMsg(text, from) =>
        context.log.info("Got message {}", text)
        //this will be sent to all 4 routees
        routerWithBroadcast ! DoBroadcastLog(text, context.self::from)
        Behaviors.same
//        val attributeactor = context.spawn(ContextProducerAttribute(), "pool-with-broadcast")
//        attributeactor ! ValueRequestMsg(text, from)
//        Behaviors.same
      case ValueResponseMsg(text,from) =>
//        context.log.info("Got message {}", text)
        from ! ValueResponseMsg(text,from)
        Behaviors.same
    }
  }
}
