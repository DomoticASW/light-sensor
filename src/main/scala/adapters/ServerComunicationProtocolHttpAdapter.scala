package adapters

import scala.concurrent.ExecutionContext
import ports.ServerComunicationProtocol.*
import sttp.model.*
import sttp.client4.quick.*
import sttp.client4.DefaultFutureBackend
import upickle.default.*
import domain.LightSensor.Event
import domain.LightSensor.LightState
import ports.ServerComunicationProtocol.ServerAddress
import scala.concurrent.Future
import domoticasw.DomoticASW
import DomoticASW.*
import upickle.core.Visitor
import scala.util.Using
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.DatagramPacket
import java.nio.charset.StandardCharsets

class ServerComunicationProtocolHttpAdapter(
    id: String,
    name: String,
    clientPort: Int,
    announcePort: Int,
    discoveryBroadcastAddress: String,
    lanHostname: String
)(using ExecutionContext)
    extends ServerComunicationProtocol:
  given Writer[Color] = Writer.derived
  given Writer[DomoticASW.ActualTypes] with
    def write0[V](out: Visitor[?, V], v: ActualTypes): V =
      v match
        case obj: Unit   => out.visitNull(-1)
        case obj: Color  => summon[Writer[Color]].write0(out, obj)
        case true        => out.visitTrue(-1)
        case false       => out.visitFalse(-1)
        case obj: Double => out.visitFloat64(obj, -1)
        case obj: Int    => out.visitInt32(obj, -1)
        case obj: String => out.visitString(obj, -1)

  import spray.json.DefaultJsonProtocol.{*, given}

  case class UpdatePropertyItem(
      propertyId: String,
      value: DomoticASW.ActualTypes
  ) derives Writer

  case class EventItem(
      event: String
  ) derives Writer

  case class LightSensorState(
      state: String
  )

  var prevState: Option[LightSensorState] = None

  override def sendEvent(address: ServerAddress, e: Event): Future[Unit] =
    quickRequest
      .httpVersion(HttpVersion.HTTP_1_1)
      .post(
        uri"http://${address.host}:${address.port}/api/devices/${this.id}/events"
      )
      .contentType(MediaType.ApplicationJson)
      .body(write(EventItem(e.toString())))
      .send(DefaultFutureBackend())
      .recoverWith(err =>
        Console.err.println(err)
        Future.failed(err)
      )
      .map(_ => ())

  override def updateState(
      address: ServerAddress,
      state: LightState
  ): Future[Unit] =
    val currentState = LightSensorState(state.name)

    prevState match
      case Some(prev) if prev == currentState => Future(())
      case _ =>
        prevState = Some(currentState)
        val updates = Seq(
          UpdatePropertyItem("state", currentState.state)
        )

        quickRequest
          .httpVersion(HttpVersion.HTTP_1_1)
          .patch(
            uri"http://${address.host}:${address.port}/api/devices/${this.id}/properties"
          )
          .contentType(MediaType.ApplicationJson)
          .body(write(updates))
          .send(DefaultFutureBackend())
          .recoverWith(err =>
            Console.err.println(err)
            Future.failed(err)
          )
          .map(_ => ())

  case class AnnounceMessage(
      id: String,
      name: String,
      lanHostname: String,
      port: Int
  ) derives Writer

  override def announce(): Unit =
    Using(DatagramSocket()): socket =>
      socket.setBroadcast(true)
      val data =
        write(AnnounceMessage(id, name, lanHostname, clientPort))
          .getBytes(StandardCharsets.UTF_8)
      val broadcastAddress = InetAddress.getByName(discoveryBroadcastAddress)
      val packet = new DatagramPacket(
        data,
        data.length,
        broadcastAddress,
        announcePort
      )
      socket.send(packet)
