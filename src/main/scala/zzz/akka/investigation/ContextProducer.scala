package zzz.akka.investigation

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.util.ByteString

import scala.util.{Failure, Success}
import scala.concurrent.Future
import akka.http.scaladsl.client.RequestBuilding.Post



object ContextProducer {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.executionContext

    val responseFuture: Future[HttpResponse] = Http().singleRequest(Post("http://127.0.0.1:8080/jobs", HttpEntity(ContentTypes.`application/json`,"""{ "id": 1, "projectName": "hola", "status": "Stats", "duration": 120}""")))

    responseFuture
      .onComplete {
        case Success(res) => println(res)
        case Failure(_)   => sys.error("something wrong")
      }
  }
}