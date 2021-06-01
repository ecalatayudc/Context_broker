package zzz.akka.contextbroker.server

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Source
import zzz.akka.contextbroker.server.ContextSupervisor.{Command, StreamMsg}


object ContextStreamAnalysis {
// analysis: numero de mensajes por segundo, media de los valores, max, min,
  def apply():Behavior[StreamMsg] = Behaviors.receiveMessage {
    case StreamMsg (values: List[List[Serializable]]) =>
        val x = values.map(_ match {
          case List(a,b) => (a,b)
        })
        println(x(0)._1)
        Behaviors.same
  }
}
