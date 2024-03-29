package zzz.akka.contextbroker.server

import akka.NotUsed
import akka.actor.{Cancellable, Scheduler}
import akka.actor.Status.Failure
import akka.actor.TypedActor.context
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.ClassicActorContextOps
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, Success}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.ActorAttributes.Dispatcher
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}
import akka.util.Timeout
import zzz.akka.contextbroker.Serializer
import zzz.akka.contextbroker.producer.ContextProducerMain.ValueAttribute
import zzz.akka.contextbroker.producer.ContextProducerPartition.{receiveMsg, spawnAttributes}
import zzz.akka.contextbroker.server.ContextServerApp.system.executionContext

import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

class Routes(buildEntitiesRepository: ActorRef[ContextSupervisor.Command], mapSource: Map[String,Map[String, Source[NotUsed.type, Cancellable]#Repr[String]#Repr[ServerSentEvent]#Repr[ServerSentEvent]]] = Map.empty)(implicit system: ActorSystem[_]) extends Serializer {

  import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
  import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
  // asking someone requires a timeout and a scheduler, if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 5.seconds

  lazy val theEntityRoutes: Route = {
    concat(
      pathPrefix("entities") {
        concat(
          pathEnd {
            concat(
              post {
                entity(as[ContextSupervisor.ContextMsg]) { entity =>
                  val operationPerformed: Future[ContextSupervisor.Response] =
                    buildEntitiesRepository.ask(ContextSupervisor.AddEntity(entity, _))
                  onSuccess(operationPerformed) {
                    case ContextSupervisor.OK         => complete("Entity added")
                    case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                  }
                }
              },
              put {
                entity(as[ContextSupervisor.ContextMsg]) { entity =>
                  val operationPerformed: Future[ContextSupervisor.Response] =
                    buildEntitiesRepository.ask(ContextSupervisor.UpdateEntity(entity, _))
                  onSuccess(operationPerformed) {
                    case ContextSupervisor.OK         => complete("Entity updated")
                    case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                  }
                }
              },
              delete {
                val operationPerformed: Future[ContextSupervisor.Response] =
                  buildEntitiesRepository.ask(ContextSupervisor.ClearEntity(_))
                onSuccess(operationPerformed) {
                  case ContextSupervisor.OK         => complete("Entities cleared")
                  case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                }
              }
            )
          },
          (get & path(Remaining)) { id =>
            println(id)
            val maybeEntity: Future[Either[Option[ContextSupervisor.ContextMsg],String]] =
              buildEntitiesRepository.ask(ContextSupervisor.GetEntityById(id, _))
            rejectEmptyResponse {
              complete(maybeEntity)
            }
          }
        )
      },
      pathPrefix("subscriptions") {
        concat(
          pathEnd {
            concat(
              post {
                entity(as[ContextSupervisor.ContextSubscription]) { subscription =>
                  val operationPerformed: Future[ContextSupervisor.Response] =
                    buildEntitiesRepository.ask(ContextSupervisor.AddSubscription(subscription, _))
                  onSuccess(operationPerformed) {
                    case ContextSupervisor.OK         => complete("Subscription added")
                    case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                  }
                }
              },
              put {
                entity(as[ContextSupervisor.ContextSubscription]) { subscription =>
                  val operationPerformed: Future[ContextSupervisor.Response] =
                    buildEntitiesRepository.ask(ContextSupervisor.UpdateSubscription(subscription, _))
                  onSuccess(operationPerformed) {
                    case ContextSupervisor.OK         => complete("Subscription updated")
                    case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                  }
                }
              }
            )
          },
          (get & path("events"/Segment/Segment)) { (idGroup,idConsumer)=>
            //eventos
            val num = new Random().between(0.0,5.0)
              complete {
                 Source
                  .tick(1.seconds, num.seconds, NotUsed)
                  .mapAsync(1) { _ =>
                    implicit val timeout: Timeout = 5.seconds
                    val response: Future[Boolean] = buildEntitiesRepository.ask(ContextSupervisor.CheckNewValues(idGroup, idConsumer, _))
                    response.collect {
                      case true => "new value"
                      case false => "no value"
                    }
                  }
                  .map(x => if (x == "new value") {
                    ServerSentEvent(x)
                  } else {
                    ServerSentEvent.heartbeat
                  })
                  .keepAlive(1.second, () => ServerSentEvent.heartbeat)
                   .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right).run()
              }


//              if (mapSource.contains(idGroup)){
//                val mapGroup = mapSource.get(idGroup).head
//                 if(mapGroup.contains(idConsumer)){
//                   mapSource.get(idGroup).head.get(idConsumer).head
//                 }else{
//                   val mapConsumer = mapSource.get(idGroup).head
//                   val newMap = mapSource.updated(idGroup,mapConsumer.+(idConsumer->source))
//                   newMap.get(idGroup).head.get(idConsumer).head
//                 }
//                  Routes.super(buildEntitiesRepository,mapSource)
//               }
          }
        )
      }
    )
  }
}
