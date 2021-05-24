package zzz.akka.contextbroker.consumer

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.unmarshalling.Unmarshal
import zzz.akka.contextbroker.server.ContextSupervisor.Job
import zzz.akka.contextbroker.server.Serializer

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ContextConsumer extends Serializer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val responseFuture: Future[HttpResponse] = Http().singleRequest(Get("http://127.0.0.1:8080/jobs/1"))

    responseFuture
      .onComplete {
        case Success(res) => Unmarshal(res).to[Job].onComplete {
          case Success(json) => println(json)
          case Failure(_) => sys.error("something wrong")
        }
        case Failure(_) => sys.error("something wrong")
      }
  }
}
