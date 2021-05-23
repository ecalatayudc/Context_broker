package zzz.akka.investigation

class DoBroadcastLog(text: String) extends ContextProducerPartition.DoLog(text)
object DoBroadcastLog {
  def apply(text: String) = new DoBroadcastLog(text)
}
