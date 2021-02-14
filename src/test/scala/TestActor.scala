import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import zzz.akka.investigation.DeviceActor
import zzz.akka.investigation.DeviceActor._

class DeviceSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {


  "Device actor" must {

    "reply with empty reading if no temperature is known" in {
      val probe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(DeviceActor("group", "device"))

      deviceActor ! DeviceActor.ReadTemperature(requestId = 42, probe.ref)
      val response = probe.receiveMessage()
      response.requestId should ===(42)
      response.value should ===(None)
    }
    "reply with latest temperature reading" in {
      val recordProbe = createTestProbe[TemperatureRecorded]()
      val readProbe = createTestProbe[RespondTemperature]()
      val deviceActor = spawn(DeviceActor("group", "device"))

      deviceActor ! DeviceActor.RecordTemperature(requestId = 1, 24.0, recordProbe.ref)
      recordProbe.expectMessage(DeviceActor.TemperatureRecorded(requestId = 1))

      deviceActor ! DeviceActor.ReadTemperature(requestId = 2, readProbe.ref)
      val response1 = readProbe.receiveMessage()
      response1.requestId should ===(2)
      response1.value should ===(Some(24.0))

      deviceActor ! DeviceActor.RecordTemperature(requestId = 3, 55.0, recordProbe.ref)
      recordProbe.expectMessage(DeviceActor.TemperatureRecorded(requestId = 3))

      deviceActor ! DeviceActor.ReadTemperature(requestId = 4, readProbe.ref)
      val response2 = readProbe.receiveMessage()
      response2.requestId should ===(4)
      response2.value should ===(Some(55.0))
    }
  }

}