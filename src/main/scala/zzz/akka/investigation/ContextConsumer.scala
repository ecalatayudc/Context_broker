package zzz.akka.investigation
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import zzz.akka.investigation.ContextSupervisor.Job

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
        case Success(res) => Unmarshal(res).to[Job].onComplete{
          case Success(json) => println(json)
          case Failure(_)   => sys.error("something wrong") }
        case Failure(_)   => sys.error("something wrong")
      }
  }
}
