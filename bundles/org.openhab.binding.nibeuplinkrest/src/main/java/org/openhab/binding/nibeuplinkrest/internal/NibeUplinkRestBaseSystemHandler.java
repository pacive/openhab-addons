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

package org.openhab.binding.nibeuplinkrest.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.model.ConnectionStatus;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;

import java.util.HashMap;
import java.util.Map;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;


/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBaseSystemHandler extends BaseThingHandler {

    private @NonNullByDefault({}) NibeUplinkRestApi nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestBaseSystemConfiguration config;

    public NibeUplinkRestBaseSystemHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        //TODO: handle commands
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        config = getConfigAs(NibeUplinkRestBaseSystemConfiguration.class);
        Bridge bridge = getBridge();
        if (bridge != null) {
            NibeUplinkRestBridgeHandler bridgeHandler = (NibeUplinkRestBridgeHandler) bridge.getHandler();
            nibeUplinkRestApi = bridgeHandler.getConnector();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, "No bridge found");
        }
        scheduler.execute(() -> {
            updateProperties();
            if (isOnline()) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    public boolean isOnline() {
        ConnectionStatus status = nibeUplinkRestApi.getSystem(config.systemId).getConnectionStatus();
        return status != ConnectionStatus.OFFLINE;
    }

    public void updateProperties() {
        Map<String, String> properties = new HashMap<>();

        NibeSystem system = nibeUplinkRestApi.getSystem(config.systemId);
        properties.put(PROPERTY_HAS_COOLING, Boolean.toString(system.hasCooling()));
        properties.put(PROPERTY_HAS_HEATING, Boolean.toString(system.hasHeating()));
        properties.put(PROPERTY_HAS_HOT_WATER, Boolean.toString(system.hasHotWater()));
        properties.put(PROPERTY_HAS_VENTILATION, Boolean.toString(system.hasVentilation()));
        properties.put(PROPERTY_SOFTWARE_VERSION, system.getSoftwareInfo().getCurrentVersion());

        thing.setProperties(properties);
    }
}
