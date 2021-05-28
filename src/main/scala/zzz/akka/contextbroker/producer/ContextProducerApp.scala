package zzz.akka.contextbroker.producer

import akka.actor.typed.ActorSystem
//actor que ejecuta la aplicacion del productor
object ContextProducerApp extends App {
  //Lista de atributos
  val attributes = List("Temperature","Temperature2","Pressure1","Pressure2")
  val system: ActorSystem[ContextProducerMain.ValueResponseMsg] =
    ActorSystem(ContextProducerMain(npartitions = 2 , att = attributes), "BuildJobsServer")
}
