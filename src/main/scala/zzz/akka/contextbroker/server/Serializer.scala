package zzz.akka.contextbroker.server

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait Serializer extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)

  import ContextSupervisor._
  import DefaultJsonProtocol._

  implicit object StatusFormat extends RootJsonFormat[Status] {
    def write(status: Status): JsValue = status match {
      case Failed => JsString("Failed")
      case Successful => JsString("Successful")
    }

    def read(json: JsValue): Status = json match {
      case JsString("Failed") => Failed
      case JsString("Successful") => Successful
      case _ => throw new DeserializationException("Status unexpected")
    }
  }

  implicit val jobFormat = jsonFormat3(ContextMsg)
}
