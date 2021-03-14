package zzz.akka.investigation

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route

import scala.concurrent.duration._
import scala.concurrent.Future

class Routes (buildJobRepository: ActorRef[ContextSupervisor.Command])(implicit system: ActorSystem[_]) extends Serializer {

  import akka.actor.typed.scaladsl.AskPattern.schedulerFromActorSystem
  import akka.actor.typed.scaladsl.AskPattern.Askable

  // asking someone requires a timeout and a scheduler, if the timeout hits without response
  // the ask is failed with a TimeoutException
  implicit val timeout: Timeout = 3.seconds

  lazy val theJobRoutes: Route =
    pathPrefix("jobs") {
      concat(
        pathEnd {
          concat(
            post {
              entity(as[ContextSupervisor.Job]) { job =>
                val operationPerformed: Future[ContextSupervisor.Response] =
                  buildJobRepository.ask(ContextSupervisor.AddJob(job, _))
                onSuccess(operationPerformed) {
                  case ContextSupervisor.OK         => complete("Job added")
                  case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
                }
              }
            },
            delete {
              val operationPerformed: Future[ContextSupervisor.Response] =
                buildJobRepository.ask(ContextSupervisor.ClearJobs(_))
              onSuccess(operationPerformed) {
                case ContextSupervisor.OK         => complete("Jobs cleared")
                case ContextSupervisor.KO(reason) => complete(StatusCodes.InternalServerError -> reason)
              }
            }
          )
        },
        (get & path(LongNumber)) { id =>
          val maybeJob: Future[Option[ContextSupervisor.Job]] =
            buildJobRepository.ask(ContextSupervisor.GetJobById(id, _))
          rejectEmptyResponse {
            complete(maybeJob)
          }
        }
      )
    }
}
