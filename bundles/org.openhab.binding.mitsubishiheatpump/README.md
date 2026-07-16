# MitsubishiHeatpump Binding

This binding allows local control of supported Mitsubishi air-ro-air heat pumps. It is based on reverse engineering of
the communication protocol by [https://github.com/muart-group](https://github.com/muart-group) and
[https://github.com/pymitsubishi](https://github.com/pymitsubishi).

## Supported Things

The binding has only one supported Thing, which represents the heapump

## Thing Configuration

The only thing required to configure is the IP address of the device on the local network. You can optionally configure
the polling interval.

### `sample` Thing Configuration

| Name            | Type    | Description                           | Default | Required | Advanced |
|-----------------|---------|---------------------------------------|---------|----------|----------|
| hostname        | text    | Hostname or IP address of the device  | N/A     | yes      | no       |
| refreshInterval | integer | Interval the device is polled in sec. | 300     | no       | yes      |

## Channels

The binding supports channels for controlling different aspects of the heatpump, as well as reading some measurements.

| Channel        | Type               | Read/Write | Description                                         |
|----------------|--------------------|------------|-----------------------------------------------------|
| power          | Switch             | RW         | Turn the heatpump on or off                         |
| mode           | Number             | RW         | The heapump operating mode                          |
| setpoint       | Number:Temperature | RW         | The temperature setpoint                            |
| fan            | Number             | RW         | Fan speed control                                   |
| verticalVane   | Number             | RW         | Direction of the vertical vane                      |
| horizontalVane | Number             | RW         | Direction of the horizontal vane                    |
| indoorTemp     | Number:Temperature | R          | Temperature measured by the indoor unit             |
| outdoorTemp    | Number:Temperature | R          | Temperature measured by the outdoor unit            |
| runtime        | Number:Time        | R          | Time the unit has been active                       |
| remoteTemp     | Number:Temperature | W          | Use an external temp sensor instead of the built-in |

If you have a temperature sensor connected to openHAB that you would like to use with the heatpump, connect it to the
remoteTemp channel using the _follow_ profile.

## Full Example

### Thing Configuration

```java
Thing mitsubishiheatpump:heatpump:exampleHeatPump "Air conditioner" @ "Livingroom" [ hostname="192.168.0.128", refreshInterval=60 ]
```

### Item Configuration

```java
Group Heatpump "Heatpump" [AirConditioner]
Switch Heatpump_Power "Heatpump Power" (Heatpump) [Control, Power] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:power" }
Number Heatpump_Mode "Mode" (Heatpump) [Control, Mode] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:mode", widget="oh-label-card" [action="options", actionItem="Heatpump_Mode"] }
Number:Temperature Heatpump_Setpoint_Temperature "Heatpump Setpoint Temperature [%.1f %unit%]" (Heatpump) [Setpoint, Temperature] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:setpoint", unit="°C" }
Number Heatpump_Fan_Mode "Heatpump Fan Mode" (Heatpump) [Airflow, Control] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:fan", widget="oh-label-card" [action="options", actionItem="Heatpump_Fan_Mode"] }
Number Heatpump_Vertical_Vane "Vertical Vane" (Heatpump) [Airflow, Control] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:verticalVane", widget="oh-label-card" [action="options", actionItem="Heatpump_Vertical_Vane"] }
Number Heatpump_Horizontal_Vane "Horizontal Vane" (Heatpump) [Airflow, Control] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:horizontalVane", widget="oh-label-card" [action="options", actionItem="Heatpump_Horizontal_Vane"] }
Number:Temperature Heatpump_Indoor_Temperature "Indoor Temperature [%.1f %unit%]" (Heatpump) [Measurement, Temperature] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:indoorTemp", unit="°C" }
Number:Temperature Heatpump_Outdoor_Temperature "Outdoor Temperature [%.1f %unit%]" (Heatpump) [Measurement, Temperature] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:outdoorTemp", unit="°C" }
Number:Time Heatpump_Runtime "Runtime [%.1f %unit%]" (Heatpump) [Duration, Measurement] { channel="mitsubishiheatpump:heatpump:exampleHeatPump:runtime", unit="h" }
Number:Temperature Livingroom_Temp "Livingroom temperature" [Measurement, Temperature] {channel="somebinding:tempsensor:somesensor:temperature", channel="mitsubishiheatpump:heatpump:exampleHeatPump:outdoorTemp" [profile="follow"] }
```
