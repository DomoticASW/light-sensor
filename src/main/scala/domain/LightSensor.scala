package domain

import state.State
import domain.LightSensor.Event
import domain.LightSensor.LightState
import domain.LightSensor.Event
import state.State
import domain.LightSensor.LightSensorState

trait LightSensor extends LightSensorState:
  def id: String
  def name: String

object LightSensor:
  enum LightState:
    case Light
    case DimLight
    case Dark

    def name: String = this match
      case Light => "Light"
      case DimLight => "Dim light"
      case Dark => "Dark"

  enum Event:
    case IsLight
    case IsDark
    case IsDimLight

  private case class LightSensorImpl(id: String, name: String) extends LightSensor with LightSensorState

  def apply(id: String, name: String): LightSensor = LightSensorImpl(id, name)

  trait LightSensorState:
    import LightState.*
    opaque type LightSensorState = (LightState, LightState)

    def initialState: LightSensorState = (Light, DimLight)
    def currentState: State[LightSensorState, LightState] =
      State(s => (s, s._1))

    /**
      * Change the state of the light sensor, meaning it detects changes on the environment light
      * e.g. => If there is light, after step() there is dimLight
      * 
      * It creates an infinite change of light state: Light -> DimLight -> Dark -> DimLight -> Light
      *
      * @return
      *   The new state with the relative Event.
      *   Possible events are: IsDark, IsLight, IsDimLight
      */
    def step(): State[LightSensorState, Event] =
      import Event.*
      State(s =>
        s match
          case (actual, Dark) => ((Light, actual), IsLight)
          case (actual, Light) => ((Dark, actual), IsDark)
          case (actual, prev) => ((prev, actual), IsDimLight)
      )

  object LightSensorStateImpl extends LightSensorState