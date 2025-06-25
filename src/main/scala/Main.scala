import ports.ServerComunicationProtocol.ServerAddress
import domain.ConfigChecker
import scala.concurrent.ExecutionContext
import domain.LightSensor
import domain.LightSensorAgent
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import adapters.ServerComunicationProtocolHttpAdapter
import adapters.DomoticASWDeviceHttpInterface

object Main extends App:
  def parse(envVar: String)(default: String): Right[Nothing, String] =
    Right(sys.env.getOrElse(envVar, default))

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

  def updateRate: Either[String, Long] =
    for
      updateRateStr <- Right(sys.env.get("UPDATE_RATE"))
      updateRate <- updateRateStr match
        case None => Right(2000L)
        case Some(value) =>
          value.toLongOption.toRight("Update rate should be an integer")
    yield updateRate

  def serverDiscoveryPort(default: Int): Either[String, Int] =
    val envVar = "SERVER_DISCOVERY_PORT"
    for
      str <- sys.env.get(envVar) match
        case None        => Right(default.toString())
        case Some(value) => Right(value)
      port <- str.toIntOption match
        case None => Left(s"Invalid port $str is not an integer")
        case Some(p) if p >= 0 & p <= 65335 => Right(p)
        case Some(p) => Left(s"Invalid port $p is out of valid port range")
    yield (port)

  val config = for
    id <- parse("ID")(default = "light-sensor")
    name <- parse("NAME")("Light Sensor")
    updateRate <- updateRate
    port <- port(default = 8080)
    serverAddress <- serverAddress(default = None)
    serverDiscoveryPort <- serverDiscoveryPort(default = 30000)
    discoveryBroadcastAddress <- parse("DISCOVERY_BROADCAST_ADDR")(default =
      "255.255.255.255"
    )
    config <- ConfigChecker(id = id, name = name, updateRate = updateRate).left
      .map(_.message)
  yield (
    config,
    port,
    serverAddress,
    serverDiscoveryPort,
    discoveryBroadcastAddress
  )

  config match
    case Left(err: String) =>
      Console.err.println(err)
      sys.exit(1)
    case Right(
          (
            config,
            port,
            serverAddress,
            serverDiscoveryPort,
            discoveryBroadcastAddress
          )
        ) =>
      val id = config.id
      val name = config.name
      val updateRate = config.updateRate
      val ec = ExecutionContext.global

      val sensor = LightSensor(id, name)
      val sensorAgent = LightSensorAgent(
        new ServerComunicationProtocolHttpAdapter(
          id,
          name = name,
          clientPort = port,
          announcePort = serverDiscoveryPort,
          discoveryBroadcastAddress = discoveryBroadcastAddress
        )(using ec),
        sensor,
        periodMs = 50,
        updateRate,
        announceEveryMs = 5000
      )
      serverAddress.foreach(sensorAgent.registerToServer(_))
      sensorAgent.start()

      given ActorSystem[Any] = ActorSystem(Behaviors.empty, "system")
      DomoticASWDeviceHttpInterface("0.0.0.0", port, sensorAgent)
