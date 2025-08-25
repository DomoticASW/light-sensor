package domain

import ports.ServerComunicationProtocol.*
import domain.LightSensor.LightSensorStateImpl.*
import state.given

/** @param lightSensor
  *   The light sensor
  * @param periodMs
  *   It is suggested to choose a period which is less than the MEST (Minimum
  *   Event Separation Time)
  * @param updateRate
  *   The rate of calling the light sensor step() function and so updating the
  *   server
  * @param announceEveryMs
  *   The rate of announcing itself to the server
  */
class LightSensorAgent(
    serverProtocol: ServerComunicationProtocol,
    val lightSensor: LightSensor,
    periodMs: Long,
    updateRate: Long,
    announceEveryMs: Long
) extends Thread:

  private var _currentLightState = initialState

  def currentLightState
      : domain.LightSensor.LightSensorStateImpl.LightSensorState =
    _currentLightState
  private def currentLightState_=(s: LightSensorState) = _currentLightState = s

  private var serverAddress: Option[ServerAddress] = None

  /** Once registered to a server the agent will send it's state once every
    * `periodMs`
    */
  def registerToServer(serverAddress: ServerAddress): Unit =
    synchronized:
      this.serverAddress = Some(serverAddress)

  def unregister(): Unit =
    synchronized:
      this.serverAddress = None

  private var _shouldStop = false
  private def shouldStop: Boolean = synchronized { _shouldStop }
  def setShouldStop(): Unit = synchronized { _shouldStop = true }

  private var timePassed: Long = 0
  private var timeFromLastAnnounceMs: Long = 0

  override def run(): Unit =
    while !shouldStop do
      Thread.sleep(periodMs)
      serverAddress match
        case Some(serverAddress) =>
          if timePassed >= updateRate
          then
            timePassed = 0
            val getStateAndEvent =
              for
                e <- step()
                s <- currentState
              yield (e, s)

            val stateAndEvent = getStateAndEvent.run(currentLightState)
            currentLightState = stateAndEvent._1

            val event = stateAndEvent._2._1
            val newState = stateAndEvent._2._2
            this.serverProtocol.updateState(serverAddress, newState)
            this.serverProtocol.sendEvent(serverAddress, event)
          else timePassed = timePassed + periodMs
        case None if timeFromLastAnnounceMs >= announceEveryMs =>
          serverProtocol.announce()
          timeFromLastAnnounceMs = 0
        case None =>
          timeFromLastAnnounceMs += periodMs
