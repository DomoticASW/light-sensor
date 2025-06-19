# A Light sensor simulated device for DomoticASW

## Run with docker

```sh
docker run corradostortini2/domoticasw-light-sensor
```

The following configurations can be passed to the container as environment variables.

For example `docker run -e NAME="MyLightSensor" -e UPDATE_RATE="1000" corradostortini2/domoticasw-light-sensor`

| Variable name  | Default value | Explanation                                         | Admissible values    |
|----------------|---------------|-----------------------------------------------------|----------------------|
| ID             | light-sensor  | Light sensor id                                     | Any not empty string |
| NAME           | Light sensor  | Light sensor name                                   | Any not empty string |
| UPDATE_RATE    | 2000          | Light sensor state update interval (ms)             | Integers > 0         |
| SERVER_ADDRESS | None          | Should be set if light sensor is already registered |                      |
| PORT           | 8080          | Port on which the device will listen                | Any valid port       |

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
