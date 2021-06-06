package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

class Routes(buildEntitiesRepository: ActorRef[ContextSupervisor.Command])(implicit system: ActorSystem[_]) extends Serializer {

  import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}

  // asking someone requires a timeout and a scheduler, if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 3.seconds
  //val pathAndPathSingleSlash = path(Segment) & pathSingleSlash
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
        }
      }
    )
  }
}
