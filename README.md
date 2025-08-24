# A Light sensor simulated device for DomoticASW

## Run with docker

```sh
docker run -e LAN_HOSTNAME=lightSensorHost corradostortini2/domoticasw-light-sensor
```

The following configurations can be passed to the container as environment variables.

For example `docker run -e LAN_HOSTNAME=lightSensorHost -e NAME="MyLightSensor" -e UPDATE_RATE="1000" corradostortini2/domoticasw-light-sensor`

| Variable name            | Default value                      | Explanation                                          | Admissible values                               |
| ------------------------ | ---------------------------------- | ---------------------------------------------------- | ----------------------------------------------- |
| ID                       | light-sensor                       | Light sensor id                                      | Any not empty string                            |
| NAME                     | Light sensor                       | Light sensor name                                    | Any not empty string                            |
| UPDATE_RATE              | 2000                               | Light sensor state update interval (ms)              | Integers > 0                                    |
| DISCOVERY_BROADCAST_ADDR | 255.255.255.255                    | Broadcast address to which send discovery announces  | Any valid broadcast address (ex: 192.168.1.255) |
| SERVER_DISCOVERY_PORT    | 30000                              | Port on which the server expects discovery announces | Any valid port                                  |
| SERVER_ADDRESS           | None                               | Should be set if light sensor is already registered  |                                                 |
| PORT                     | 8080                               | Port on which the device will listen                 | Any valid port                                  |
| \*LAN_HOSTNAME           | None, it is mandatory to be passed | LAN hostname that will resolve to the device ip      | Any valid hostname (ex: lightSensor1234)        |

## Properties

The properties of the light sensor are:

- state (The state of the perceived light, that can be: Light, Dim Light or Dark)

## Events

The events of the light sensor are:

- ToLight (Whenever the state property changes to light)
- ToDimLight (Whenever the state property changes to dim light)
- ToDark (Whenever the state property changes to dark)

## Actions

The light sensor has not actions, it is just a sensor that detect changes in the environment automatically (the emulated one just really change state whenever UPDATE_RATE time passes, following an infinite loop of the following type: LIGHT -> DIM_LIGHT -> DARK -> DIM_LIGHT -> LIGHT -> ...)
