package zzz.akka.contextbroker

import akka.NotUsed
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy, Terminated}
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.util.ByteString

import scala.util.{Failure, Success}
import scala.concurrent.Future
import akka.http.scaladsl.client.RequestBuilding.Post
import javafx.concurrent.Worker
import zzz.akka.contextbroker.ContextProducerMain.ValueRequestMsg
import zzz.akka.contextbroker.ContextProducerPartition.{Command, DoLog}



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
        case ValueRequestMsg(text,from) =>
          //#create-actors
          val poolWithBroadcast = pool.withBroadcastPredicate(_.isInstanceOf[DoBroadcastLog])
          val routerWithBroadcast = ctx.spawn(poolWithBroadcast, "pool-with-broadcast")
          //this will be sent to all 4 routees
          routerWithBroadcast ! DoBroadcastLog(text,from)
          Behaviors.empty
      }
    }
  }
