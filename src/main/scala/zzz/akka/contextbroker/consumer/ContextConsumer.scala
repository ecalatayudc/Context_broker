package zzz.akka.contextbroker.consumer

import akka.NotUsed
import akka.actor.ActorSystem

import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Get
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import zzz.akka.contextbroker.server.ContextSupervisor.ContextMsg
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.client.RequestBuilding.Put
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.stream.scaladsl.Source
import zzz.akka.contextbroker.Serializer
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._

object ContextConsumer extends Serializer {


  def main(args: Array[String]): Unit = {

    val sub = "{entities:[{id:Room1,type:Room}], condition:{attrs:[pressure1]}}"
    val not = "{http:{url:http://localhost:1028/accumulate}, attrs:[temperature1,temperature2]}"
    sendSubscription("Group1","consumer1","sub to Room1",sub,not,"2040-01-01T14:00:00.00Z","5")
  }
  private def sendSubscription( idGroup: String, idConsumer: String, desc: String,sub: String, not: String, exp: String, thr: String):Unit = {
    implicit val system= ActorSystem()
    implicit val mat    = ActorMaterializer
    implicit val dispatcher = system.dispatcher
    import akka.http.scaladsl.unmarshalling.sse.EventStreamUnmarshalling._
    import system.dispatcher

    // needed for the future flatMap/onComplete in the end
    val json_response = s"""{ "idGroup": "$idGroup","idConsumer": "$idConsumer", "description": "$desc", "subject": "$sub", "notification": "$not", "expires": "$exp", "throttling": "$thr" }"""
//    val responseFuture: Future[HttpResponse] = Http().singleRequest(Post("http://127.0.0.1:5804/subscriptions", HttpEntity(ContentTypes.`application/json`, json_response)))
    val responseFuture: Future[HttpResponse] = Http().singleRequest(Put("http://127.0.0.1:5804/subscriptions", HttpEntity(ContentTypes.`application/json`, json_response)))
    responseFuture
      .onComplete {
        case Success(res) =>
            println(res)
          //request para eventos
          Http()
              .singleRequest(Get("http://127.0.0.1:5804/subscriptions/events"))
              .flatMap(Unmarshal(_).to[Source[ServerSentEvent, NotUsed]])
              .foreach(_.runForeach(println))
        case Failure(_) => sys.error("something wrong")
      }
  }
  private def getMsg():Unit = {
    implicit val system= ActorSystem()
      implicit val mat    = ActorMaterializer
      implicit val dispatcher = system.dispatcher
    val responseFuture: Future[HttpResponse] = Http().singleRequest(Get("http://127.0.0.1:5804/entities/Room1/"))

    responseFuture
      .onComplete {
        case Success(res) => Unmarshal(res).to[ContextMsg].onComplete {
          case Success(json) => println(json)
          case Failure(_) => sys.error("something wrong")
        }
        case Failure(_) => sys.error("something wrong")
      }
  }

}
