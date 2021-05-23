package zzz.akka.investigation

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
import zzz.akka.investigation.ContextProducer.ValueRequestMsg
import zzz.akka.investigation.ContextProducerPartition.{Command, DoLog}



object ContextProducerRouter {

  //  def main(args: Array[String]): Unit = {
  //    implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
  //    // needed for the future flatMap/onComplete in the end
  //    implicit val executionContext = system.executionContext
  //    val json_response = """{ "id": 1, "projectName": "hola", "status": "Stats", "duration": 120}"""
  //    val responseFuture: Future[HttpResponse] = Http().singleRequest(Post("http://127.0.0.1:8080/jobs", HttpEntity(ContentTypes.`application/json`,json_response)))
  //
  //    responseFuture
  //      .onComplete {
  //        case Success(res) => println(res)
  //        case Failure(_)   => sys.error("something wrong")
  //      }
  //  }

    def apply(): Behavior[ValueRequestMsg] = Behaviors.setup[ValueRequestMsg] { ctx =>
      val pool = Routers.pool(poolSize = 4) {
        // make sure the workers are restarted if they fail
        Behaviors.supervise(ContextProducerPartition()).onFailure[Exception](SupervisorStrategy.restart)
      }
      //      val router = ctx.spawn(pool, "worker-pool")

      //      (0 to 10).foreach { n =>
      //        router ! ContextProducerPartition.DoLog(s"msg $n")
      //      }
      Behaviors.receiveMessage {
        case ValueRequestMsg(text) =>
          //#create-actors
          val poolWithBroadcast = pool.withBroadcastPredicate(_.isInstanceOf[DoBroadcastLog])
          val routerWithBroadcast = ctx.spawn(poolWithBroadcast, "pool-with-broadcast")
          //this will be sent to all 4 routees
          routerWithBroadcast ! DoBroadcastLog(text)
          Behaviors.empty
      }
    }
  }
