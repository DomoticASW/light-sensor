import ports.ServerComunicationProtocol.ServerAddress
import domain.ConfigChecker
import scala.concurrent.ExecutionContext
import domain.LightSensor
import domain.LightSensorAgent
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import adapters.ServerComunicationProtocolHttpAdapter

object Main extends App:
  object isInt:
    def unapply(s: String): Option[Int] = s.toIntOption

  def serverAddress(
      default: Option[ServerAddress]
  ): Either[String, Option[ServerAddress]] =
    def stringToServerAddress(s: String): Either[String, ServerAddress] =
      s.split(":").toList match
        case host :: (isInt(port) :: next) => Right(ServerAddress(host, port))
        case _ => Left(s"Invalid server address \"$s\"")

    for
      serverAddressStr <- Right(sys.env.get("SERVER_ADDRESS"))
      serverAddress <- serverAddressStr match
        case Some(value) => stringToServerAddress(value).map(Some(_))
        case None        => Right(default)
    yield (serverAddress)

  def port(default: Int): Either[String, Int] =
    sys.env.get("PORT") match
      case None                                  => Right(default)
      case Some(isInt(p)) if p >= 0 & p <= 65335 => Right(p)
      case Some(isInt(p)) => Left(s"Invalid port $p is out of valid port range")
      case Some(nonInt)   => Left(s"Invalid port $nonInt is not an integer")

  def id: Either[String, String] = Right(sys.env.get("ID").getOrElse("light-sensor"))
  def lightSensorName: Either[String, String] = Right(sys.env.get("NAME").getOrElse("Light Sensor"))

  def updateRate: Either[String, Long] =
    for
      updateRateStr <- Right(sys.env.get("UPDATE_RATE"))
      updateRate <- updateRateStr match
        case None => Right(2000l)
        case Some(value) =>
          value.toLongOption.toRight("Update rate should be an integer")
    yield updateRate

  val config = for
    id <- id
    name <- lightSensorName
    updateRate <- updateRate
    port <- port(default = 8080)
    serverAddress <- serverAddress(default = None)
    config <- ConfigChecker(id = id, name = name, updateRate = updateRate).left.map(_.message)
  yield (config, port, serverAddress)

  config match
    case Left(err: String) =>
      Console.err.println(err)
      sys.exit(1)
    case Right((config, port, serverAddress)) =>
      val id = config.id
      val name = config.name
      val updateRate = config.updateRate
      val ec = ExecutionContext.global

      val sensor = LightSensor(id, name)
      val sensorAgent = LightSensorAgent(new ServerComunicationProtocolHttpAdapter(id)(using ec), sensor, 50, updateRate)
      serverAddress.foreach(sensorAgent.registerToServer(_))
      sensorAgent.start()

      given ActorSystem[Any] = ActorSystem(Behaviors.empty, "system")
      