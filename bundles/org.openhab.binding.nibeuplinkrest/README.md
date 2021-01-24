# NibeUplinkRest Binding

This binding connects to the Nibe Uplink public REST API to integrate Nibe heatpumps.



## Supported Things

The binding supports a Bridge that handles the connection to Nibe uplink, a System thing that gets
automatically created by querying the API and creates a corresponding OpenHAB Thing, and a Thermostat
Thing that can control the temperature for the heating system.

## Discovery

The bridge and thermostat needs to be configured manually. After the Bridge is created it can automatically
discover System Things with channels depending on the configuration Nibe Uplink reports.

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

## Channels

_Here you should provide information about available channel types, what their meaning is and how they can be used._

_Note that it is planned to generate some part of this based on the XML files within ```src/main/resources/ESH-INF/thing``` of your binding._

| channel  | type   | description                  |
|----------|--------|------------------------------|
| control  | Switch | This is the control channel  |

## Full Example

_Provide a full usage example based on textual configuration files (*.things, *.items, *.sitemap)._

## Any custom content here!

_Feel free to add additional sections for whatever you think should also be mentioned about your binding!_
