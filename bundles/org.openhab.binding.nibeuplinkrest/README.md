# NibeUplinkRest Binding

This binding connects to the Nibe Uplink public REST API to integrate Nibe heatpumps.


## Supported Things

The binding supports a Bridge that handles the connection to Nibe uplink, a System thing that gets
automatically created by querying the API and creates a corresponding OpenHAB Thing, and a Thermostat
Thing that can control the temperature for the heating system.

## Discovery

The bridge needs to be configured manually. After the Bridge is created it can automatically
discover System Things with channels depending on the configuration Nibe Uplink reports.
For every climate system the heatpump controls, a Thermostat Thing is also added to the inbox.

## Thing Configuration

To use the binding you first need a developer account at Nibe Uplink.
You can create one by logging in to https://api.nibeuplink.com with your regular
Nibe Uplink account.

After that you need to create an Application: go to "My Applications" and click "Create application".
Fill in a name and description as you prefer. For the Callback URL you need to input 
`http(s)://<YOUR-OPENHAB-ADDRESS>:<PORT>/nibeuplinkconnect`. This doesn't need to be accessible from
outside your network, but needs to be reachable with a browser. E.g if your OpenHAB is running on a computer
with ip-address 192.168.0.100 you can input `http://192.168.0.100:8080/nibeuplinkconnect`. If it runs on the same
computer that you are using you can input `http://localhost:8080/nibeuplinkconnect`

After the application is created, create a NibeUplink REST API Bridge and fill in the Client Identifier and
Client Secret you recieved for your application. Then use a browser to navigate to the address you typed in as
Callback URL. Your bridge should show up with an "Authorize" button. Clicking it will redirect you to a Nibe Uplink
login page, where you need to authorize access to your account. After you have done that you will be redirected back
to the previous page, which should state that the Bridge is authorized.

Finally, go back to the OpenHAB UI and scan for Things. Your system should show up and can be added.
Note that there might be a lot of channels created, but some are hidden, you need to click "Show advanced" to see all
of them.

### Thermostat

A Thermostat Thing acts as a virtual thermostat by reporting current room temperature and target temperature to Nibe
Uplink. This can the be used by your heatpump to adjust the heat medium flow temp. How much it is affected depends on
settings in your heatpump.

Thermostat Things need to be configured manually. When creating a new Thermostat, the following parameters need to be
configured:

| Parameter | Description |
|-----------|-------------|
| Bridge | The bridge that is connected to Nibe Uplink |
| Id | A numeric value that identifies the thermostat on Nibe Uplink's side |
| System Id | The Id of the heating system connected to Nibe Uplink (if you have added a system Thing to OH, use the systemId parameter of that) |
| Thermostat Name | A descriptive name that is reported to Nibe Uplink |
| Climate Systems | Select the climate systems that should be controlled by this thermostat |

## Channels

The binding automatically creates channel groups and channels based on the components reported by Nibe uplink.
This means that even if your system has some external component it should show up without any configuration needed on
your part. This also means that the available channels might be different depending on your system, so they cannot all
be listed here. 

The channels are grouped based on the components that Nibe Uplink reports, but to reduce clutter in the UI, many of them
are hidden by default, but can be seen by checking the "Show advanced" box at the top of the list. By default the
following groups are displayed:

* Status - General information e.g:
  - Outdoor temp
  - Hot water temp
  - Status of different components (Compressor, ventilation, electric addon etc)
  - Alarm info
  - Software info
* Climate system (one group for each installed system)
  - Heat medium flow/calculated/return temperature
  - Room temperature
* Control (see below)

Nibe Uplink report all values as integers, that needs to be scaled down (e.g a temperature of 21.5 is reported as 215). When
the channels are created the binding tries to calculate the correct scaling factor (either 1, 10 or 100), but might
miss some times. If you notice that this happens the scaling factor can be overridden in the channel configuration.

### Control channels

The channels in the control group can be used to control some features of the system. Most of these are temporary, and
the system will revert to the default state either after a set period of time or if OpenHAB or your Heatpump loses connection
to Nibe Uplink for more than 30 minutes.

| Channel | Description | Notes|
|---------|-------------|------|
| Mode | Sets the system to vacation mode or away mode | Reverts if connection is lost |
| Ventilation boost | Sets the fan speed to one of four predefined speeds, or the default speed | Reverts to default after a predefined period. Only available for systems with ventilation |
| Hot water boost | Sets the hot water to luxury mode for a set period of time | Only available for systems with hot water |
| Parallel adjustment when heating | Sets parallel adjustment of the heating curve | One for each climate system. Only available for systems with heating |
| Target temperature when heating | Sets target temperature for the heating system | One for each climate system. Only available for systems with heating |
| Parallel adjustment when cooling | Sets parallel adjustment of the heating curve | One for each climate system. Only available for systems with cooling |
| Target temperature when cooling | Sets target temperature for the heating system | One for each climate system. Only available for systems with cooling |

### Thermostats

A Thermostat thing have two channels: Current temperature and Target temperature. When a command is sent to either
of these channels, they are forwarded to Nibe Uplink and then to your system, which can adjust the heating need
accordingly. A good way to configure the Current temperature channel is to use the follow-profile with an Item that is
connected to a temp sensor. When the Item receives an update from the sensor, the virtual thermostat in Nibe Uplink
will be updated. 

If you have a thermostat connected to OpenHAB, the Target temperature can be configured in the same
way, which lets you control your system from the thermostat directly. If not, you can connect it to an Item to make it
controllable from OpenHAB. If the Target temperature is not set, the system  will use it's configured value instead.

If either OpenHAB or your heatpump loses connection to Nibe Uplink, the heatpump reverts to its default values after 30
minutes.
