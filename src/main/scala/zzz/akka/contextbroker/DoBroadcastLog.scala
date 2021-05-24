package zzz.akka.contextbroker

import akka.actor.typed.ActorRef


class DoBroadcastLog(text: String, from: ActorRef[ContextProducerMain.ValueResponseMsg]) extends ContextProducerPartition.DoLog(text,from)
object DoBroadcastLog {
  def apply(text: String, from: ActorRef[ContextProducerMain.ValueResponseMsg]) = new DoBroadcastLog(text, from: ActorRef[ContextProducerMain.ValueResponseMsg])
}
