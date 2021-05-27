package zzz.akka.contextbroker.producer

import akka.actor.typed.scaladsl.{Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.{ContentTypes, HttpResponse}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._


object ContextProducerMain {

  sealed trait ValueAttribute
  case class ValueRequestMsg(text: String, from: ActorRef[ValueResponseMsg]) extends ValueAttribute
  case class ValueResponseMsg(text: String, from: ActorRef[ValueResponseMsg]) extends ValueAttribute
  final case class AggregatedQuotes(quotes: List[ValueAttribute],from: ActorRef[ValueResponseMsg]) extends ValueAttribute
  final case object GetValue extends ValueAttribute


  def apply(npartitions: Int, att: List[String]): Behavior[ValueAttribute] = {
    system(npartitions, att)
  }

  private def system(np: Int, natt: List[String]): Behavior[ValueAttribute] = Behaviors.setup { ctx =>
    val routerWithBroadcast = ctx.spawn(ContextProducerRouter(np, natt,ctx.self), "pool-with-broadcast")
    routerWithBroadcast ! ValueRequestMsg("msg_broadcast_producer", ctx.self)
    Behaviors.receiveMessage {
      case ValueResponseMsg(text,_) =>
        ctx.log.info("Got message {}", text)
        (0 to 10).foreach{ n=>
          sendValue(text,n)
        }
        Behaviors.same
      case _ => Behaviors.same
    }
  }
  private def sendValue(value: String,id: Int):Unit = {
        import scala.util.{Failure, Success}
        import scala.concurrent.Future
        import akka.http.scaladsl.client.RequestBuilding.Post
        implicit val system = ActorSystem(Behaviors.empty, "SingleRequest")
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext = system.executionContext
        val json_response = s"""{ "id": $id, "projectName": "$value", "status": "Stats", "duration": 120}"""
        val responseFuture: Future[HttpResponse] = Http().singleRequest(Post("http://127.0.0.1:8080/jobs", HttpEntity(ContentTypes.`application/json`, json_response)))

        responseFuture
          .onComplete {
            case Success(res) => println(res)
            case Failure(_) => sys.error("something wrong")
          }
      }
}
