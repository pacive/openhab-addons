/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.openhab.binding.nibeuplinkrest.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.CHANNEL_THERMOSTAT_CURRENT;
import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.CHANNEL_THERMOSTAT_TARGET;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestThermostatHandler extends BaseThingHandler {

    private @NonNullByDefault({}) NibeUplinkRestApi nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestThermostatConfiguration config;

    private @Nullable Double currentTemperature;
    private @Nullable Double targetTemperature;


    public NibeUplinkRestThermostatHandler(Thing thing) {
        super(thing);
    }

    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        config = getConfigAs(NibeUplinkRestThermostatConfiguration.class);
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getHandler() != null) {
            NibeUplinkRestBridgeHandler bridgeHandler = (NibeUplinkRestBridgeHandler) bridge.getHandler();
            nibeUplinkRestApi = bridgeHandler.getApiConnection();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, "No bridge found");
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof State) {
            handleUpdate(channelUID, (State) command);
        }
    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {
        if (newState instanceof DecimalType) {
            switch (channelUID.getId()) {
                case CHANNEL_THERMOSTAT_CURRENT:
                    currentTemperature = ((DecimalType) newState).doubleValue();
                    break;
                case CHANNEL_THERMOSTAT_TARGET:
                    targetTemperature = ((DecimalType) newState).doubleValue();
                    break;
            }
        }
        Thermostat thermostat = new Thermostat(config.id, config.name, config.getClimateSystems(),
                    currentTemperature, targetTemperature);
        nibeUplinkRestApi.setThermostat(config.systemId, thermostat);
    }
}
