package domain

import state.given
import ports.ServerComunicationProtocol.*
import domain.LightSensor.LightSensorStateImpl.*
import state.given

/** @param lightSensor
  *   The light sensor
  * @param periodMs
  *   It is suggested to choose a period which is less than the MEST (Minimum
  *   Event Separation Time)
  * @param updateRate
  *   The rate of calling the light sensor step() function and so updating the server
  */
class LightSensorAgent(serverProtocol: ServerComunicationProtocol, lightSensor: LightSensor, periodMs: Long, updateRate: Long) extends Thread:

  private var serverAddress: Option[ServerAddress] = None

  /** Once registered to a server the agent will send it's state once every
    * `periodMs`
    */
  def registerToServer(serverAddress: ServerAddress): Unit =
    synchronized:
      this.serverAddress = Some(serverAddress)

  private var _shouldStop = false
  private def shouldStop: Boolean = synchronized { _shouldStop }
  def setShouldStop(): Unit = synchronized { _shouldStop = true }

  private var timePassed: Long = 0

  override def run(): Unit =
    _run(initialState)

  private def _run(s: LightSensorState): Unit =
    if !shouldStop then
      Thread.sleep(periodMs)
      timePassed = timePassed + periodMs
      if timePassed >= updateRate
      then
        timePassed = 0
        val state = 
          for
            e <- step()
            s <- currentState
          yield (e, s)

        val computation = state.run(s)
        val event = computation._2._1
        val newState = computation._2._2
        this.serverAddress.foreach(this.serverProtocol.updateState(_, newState))
        this.serverAddress.foreach(this.serverProtocol.sendEvent(_, event))
        println("State: " + newState)
        _run(computation._1)
