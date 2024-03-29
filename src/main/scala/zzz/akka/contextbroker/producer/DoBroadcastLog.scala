package zzz.akka.contextbroker.producer

import akka.actor.typed.ActorRef
import zzz.akka.contextbroker.producer.ContextProducerMain.{ValueAttribute, ValueRequestMsg, ValueResponseMsg}


class DoBroadcastLog(text: String, from: ActorRef[ValueResponseMsg]) extends ContextProducerMain.ValueRequestMsg(text,from)
object DoBroadcastLog {
  def apply(text: String, from: ActorRef[ValueResponseMsg]) = new DoBroadcastLog(text, from: ActorRef[ValueResponseMsg])
}
