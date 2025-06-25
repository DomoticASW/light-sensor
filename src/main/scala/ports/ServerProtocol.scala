package ports

import scala.concurrent.Future
import domain.LightSensor.Event
import domain.LightSensor.LightState

object ServerComunicationProtocol:
  case class ServerAddress(host: String, port: Int)

  trait ServerComunicationProtocol:
    def updateState(address: ServerAddress, state: LightState): Future[Unit]
    def sendEvent(address: ServerAddress, e: Event): Future[Unit]
    def announce(): Unit
