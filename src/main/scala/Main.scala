import domain.LightSensor.*
import LightSensorStateImpl.*
import state.given

object Main extends App:
  val state = (for 
    _ <- step()
    _ <- step()
    _ <- step()
    e <- step()
  yield e)

  println(state.run(initialState))