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