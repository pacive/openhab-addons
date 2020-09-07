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

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.CHANNEL_THERMOSTAT_CURRENT;
import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.CHANNEL_THERMOSTAT_TARGET;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Thermostat;
import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Handles virtual thermostat Things connecting openHAB to Nibe uplink
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestThermostatHandler extends BaseThingHandler {

    private @NonNullByDefault({}) NibeUplinkRestApi nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestThermostatConfiguration config;

    private final ItemChannelLinkRegistry itemChannelLinkRegistry;

    private @Nullable Double currentTemperature;
    private @Nullable Double targetTemperature;

    public NibeUplinkRestThermostatHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing);
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    public void initialize() {
        config = getConfigAs(NibeUplinkRestThermostatConfiguration.class);
        Bridge bridge = getBridge();
        if (bridge != null) {
            NibeUplinkRestBridgeHandler bridgeHandler = (NibeUplinkRestBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                nibeUplinkRestApi = bridgeHandler.getApiConnection();
            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED);
                return;
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge configured");
            return;
        }
        scheduler.execute(() -> {
            readConnectedItem(CHANNEL_THERMOSTAT_CURRENT);
            readConnectedItem(CHANNEL_THERMOSTAT_TARGET);
            nibeUplinkRestApi.setThermostat(config.systemId, createThermostat());
            updateStatus(ThingStatus.ONLINE);
        });
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof DecimalType) {
            switch (channelUID.getId()) {
                case CHANNEL_THERMOSTAT_CURRENT:
                    if (currentTemperature == null || ((DecimalType) command).doubleValue() != currentTemperature) {
                        currentTemperature = ((DecimalType) command).doubleValue();
                        nibeUplinkRestApi.setThermostat(config.systemId, createThermostat());
                    }
                    break;
                case CHANNEL_THERMOSTAT_TARGET:
                    if (targetTemperature == null || ((DecimalType) command).doubleValue() != targetTemperature) {
                        targetTemperature = ((DecimalType) command).doubleValue();
                        nibeUplinkRestApi.setThermostat(config.systemId, createThermostat());
                    }
                    break;
            }
        }
    }

    @Override
    public void handleRemoval() {
        nibeUplinkRestApi.removeThermostat(config.systemId, config.id);
        super.handleRemoval();
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        readConnectedItem(channelUID);
    }

    /**
     * Create a {@link Thermostat} from the current values
     *
     * @return A {@link Thermostat}
     */
    public Thermostat createThermostat() {
        return new Thermostat(config.id, config.name, config.climateSystems, currentTemperature, targetTemperature);
    }

    /**
     * Read the value of the first connected {@link Item} and updates the cached value,
     * in case the Item doesn't get a timely update
     *
     * @param channelID The id of the channel
     */
    private void readConnectedItem(String channelID) {
        ChannelUID channelUID = new ChannelUID(thing.getUID(), channelID);
        readConnectedItem(channelUID);
    }

    /**
     * Read the value of the first connected {@link Item} and updates the cached value,
     * in case the Item doesn't get a timely update
     *
     * @param channelUID {@link ChannelUID} of the channel
     */
    private void readConnectedItem(ChannelUID channelUID) {
        Set<Item> connectedItems = itemChannelLinkRegistry.getLinkedItems(channelUID);
        if (!connectedItems.isEmpty()) {
            State state = connectedItems.iterator().next().getState();
            if (state instanceof DecimalType) {
                switch (channelUID.getId()) {
                    case CHANNEL_THERMOSTAT_CURRENT:
                        currentTemperature = ((DecimalType) state).doubleValue();
                        break;
                    case CHANNEL_THERMOSTAT_TARGET:
                        targetTemperature = ((DecimalType) state).doubleValue();
                }
            }
        }
    }
}
