package zzz.akka.contextbroker.server

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directives, Route}
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration._

class Routes (buildJobRepository: ActorRef[ContextSupervisor.Command])(implicit system: ActorSystem[_]) extends Serializer {

  import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}

  // asking someone requires a timeout and a scheduler, if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 3.seconds
  //val pathAndPathSingleSlash = path(Segment) & pathSingleSlash
  lazy val theJobRoutes: Route = {
    pathPrefix("entities") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[ContextSupervisor.ContextMsg]) { job =>
                val operationPerformed: Future[ContextSupervisor.Response] =
                  buildJobRepository.ask(ContextSupervisor.AddEntity(job, _))
                onSuccess(operationPerformed) {
                  case ContextSupervisor.OK         => complete("Job added")
                  case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                }
              }
            },
            delete {
              val operationPerformed: Future[ContextSupervisor.Response] =
                buildJobRepository.ask(ContextSupervisor.ClearEntity(_))
              onSuccess(operationPerformed) {
                case ContextSupervisor.OK         => complete("Jobs cleared")
                case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
              }
            }
          )
        },
        (get & path(Remaining)) { id =>
          println(id)
          val maybeJob: Future[Option[ContextSupervisor.ContextMsg]] =
            buildJobRepository.ask(ContextSupervisor.GetEntityById(id, _))
          rejectEmptyResponse {
            complete(maybeJob)
          }
        }
      )
    }
  }
}
