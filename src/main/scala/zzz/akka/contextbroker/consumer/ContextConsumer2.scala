package zzz.akka.contextbroker.consumer

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{Get, Post, Put}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import zzz.akka.contextbroker.Serializer
import zzz.akka.contextbroker.server.ContextSupervisor.ContextMsg

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ContextConsumer2 extends Serializer {


  def main(args: Array[String]): Unit = {

    val sub = "{entities:[{id:Room1,type:Room}], condition:{attrs:[pressure2]}}"
    val not = "{http:{url:http://localhost:1028/accumulate}, attrs:[temperature1,temperature2]}"
    sendSubscription("Group1","consumer2","sub to Room1",sub,not,"2040-01-01T14:00:00.00Z","5")
  }
  private def sendSubscription( idGroup: String, idConsumer: String, desc: String,sub: String, not: String, exp: String, thr: String):Unit = {
    implicit val system= ActorSystem()
    implicit val mat    = ActorMaterializer
    implicit val dispatcher = system.dispatcher
    import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._

    // needed for the future flatMap/onComplete in the end
    val json_response = s"""{ "idGroup": "$idGroup","idConsumer": "$idConsumer", "description": "$desc", "subject": "$sub", "notification": "$not", "expires": "$exp", "throttling": "$thr" }"""
    val responseFuture: Future[HttpResponse] = Http().singleRequest(Post("http://127.0.0.1:5804/subscriptions", HttpEntity(ContentTypes.`application/json`, json_response)))
//    val responseFuture: Future[HttpResponse] = Http().singleRequest(Put("http://127.0.0.1:5804/subscriptions", HttpEntity(ContentTypes.`application/json`, json_response)))
    responseFuture
      .onComplete {
        case Success(res) =>
            println(res)
          //request para eventos
          Http()
              .singleRequest(Get(s"http://127.0.0.1:5804/subscriptions/events/$idGroup/$idConsumer"))
              .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
              .foreach(_.runForeach(getMsg(_)))
        case Failure(_) => sys.error("something wrong")
      }
  }
  private def getMsg(s: ServerSentEvent):Unit = {
    implicit val system= ActorSystem()
      implicit val mat    = ActorMaterializer
      implicit val dispatcher = system.dispatcher
    val responseFuture: Future[HttpResponse] = Http().singleRequest(Get("http://127.0.0.1:5804/entities/Room1/"))

    responseFuture
      .onComplete {
        case Success(res) => Unmarshal(res).to[ContextMsg].onComplete {
          case Success(json) =>
            println(s)
            println(json)
          case Failure(_) => sys.error("something wrong")
        }
        case Failure(_) => sys.error("something wrong")
      }
  }

}
