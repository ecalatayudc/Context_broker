package zzz.akka.contextbroker.producer

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.model.StatusCodes.Success
import akka.http.scaladsl.model.{ContentTypes, HttpResponse}
import akka.actor.{Actor, ActorLogging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.util.ByteString
import scala.concurrent.Future
import scala.util.Failure

object ContextProducerMain {

//  def main(args: Array[String]): Unit = {
//    import scala.util.{Failure, Success}
//    import scala.concurrent.Future
//    import akka.http.scaladsl.client.RequestBuilding.Post
//    implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
//    // needed for the future flatMap/onComplete in the end
//    implicit val executionContext = system.executionContext
//    val json_response = """{ "id": 1, "projectName": "hola", "status": "Stats", "duration": 120}"""
//    val responseFuture: Future[HttpResponse] = Http().singleRequest(Post("http://127.0.0.1:8080/jobs", HttpEntity(ContentTypes.`application/json`, json_response)))
//
//    responseFuture
//      .onComplete {
//        case Success(res) => println(res)
//        case Failure(_) => sys.error("something wrong")
//      }
//  }
//}
  sealed trait ValueAttribute

  case class ValueRequestMsg(text: String, from: ActorRef[ValueResponseMsg]) extends ValueAttribute

  case class ValueResponseMsg(text: String) extends ValueAttribute

  def apply(npartitions: Int, nattributes: Int): Behavior[ValueResponseMsg] = {
    system(npartitions, nattributes)

  }

  private def system(np: Int, natt: Int): Behavior[ValueResponseMsg] = Behaviors.setup { ctx =>
    val routerWithBroadcast = ctx.spawn(ContextProducerRouter(np, natt), "pool-with-broadcast")
    //this will be sent to all 4 routees
    routerWithBroadcast ! ValueRequestMsg("msg_broadcast_producer", ctx.self)
    Behaviors.receiveMessage { message =>
      val values = message.text + 1
      ctx.log.info("Got message {}", message.text)
      Behaviors.same
    }
  }
}
