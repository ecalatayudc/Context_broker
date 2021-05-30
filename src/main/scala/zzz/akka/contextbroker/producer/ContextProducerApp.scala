package zzz.akka.contextbroker.producer

import akka.actor.typed.ActorSystem
//actor que ejecuta la aplicacion del productor
object ContextProducerApp extends App {
  //Lista de atributos
  val attributes = List("temperature1","temperature2","pressure1","pressure2")
  val entity = "Room1"
  val entityType = "Room"
  val system: ActorSystem[ContextProducerMain.ValueResponseMsg] =
    ActorSystem(ContextProducerMain(npartitions = 2 , att = attributes, ent = entity, entType = entityType), "BuildJobsServer")
}
