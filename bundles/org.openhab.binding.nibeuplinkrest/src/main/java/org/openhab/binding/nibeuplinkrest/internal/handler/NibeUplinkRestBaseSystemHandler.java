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
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestCallbackListener;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestChannelGroupTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;


/**
 * Handles Things for a heating system connected to Nibe uplink
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBaseSystemHandler extends BaseThingHandler implements NibeUplinkRestCallbackListener {

    private @NonNullByDefault({}) NibeUplinkRestApi nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestBaseSystemConfiguration config;
    private @NonNullByDefault({}) int systemId;
    private final NibeUplinkRestChannelGroupTypeProvider groupTypeProvider;

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestBaseSystemHandler.class);

    public NibeUplinkRestBaseSystemHandler(Thing thing, NibeUplinkRestChannelGroupTypeProvider groupTypeProvider) {
        super(thing);
        this.groupTypeProvider = groupTypeProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String groupId = channelUID.getGroupId();
        String channelId = channelUID.getIdWithoutGroup();
        // Only channels in the control channel group can be commanded
        if (groupId.equals(CHANNEL_GROUP_CONTROL_ID)) {
            if (channelId.equals(CHANNEL_MODE_ID) && command instanceof StringType) {
                nibeUplinkRestApi.setMode(systemId, Mode.from(command.toFullString()));
                // All channels except for mode accept numbers as command
            } else if (command instanceof DecimalType) {
                try {
                    Map<Integer, Integer> parameter = new HashMap<>();
                    parameter.put(Integer.parseInt(channelId), ((DecimalType) command).intValue());
                    nibeUplinkRestApi.setParameters(systemId, parameter);
                } catch (Exception e) {
                    logger.warn("Failed to send command to channel {} with value {}. Reason: {}",
                            channelId, command.toFullString(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing thing {}", thing.getUID().getAsString());
        updateStatus(ThingStatus.UNKNOWN);
        config = getConfigAs(NibeUplinkRestBaseSystemConfiguration.class);
        systemId = config.systemId;
        Bridge bridge = getBridge();
        if (bridge != null) {
            NibeUplinkRestBridgeHandler bridgeHandler = (NibeUplinkRestBridgeHandler) bridge.getHandler();
            if (bridgeHandler != null) {
                nibeUplinkRestApi = bridgeHandler.getApiConnection();
                nibeUplinkRestApi.addCallbackListener(systemId, this);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, "No bridge found");
        }
        scheduler.execute(() -> {
            updateProperties();
            thing.getChannels().stream().filter(c -> isLinked(c.getUID())).forEach(c -> {
                try {
                    nibeUplinkRestApi.addTrackedParameter(systemId, Integer.parseInt(c.getUID().getIdWithoutGroup()));
                } catch (NumberFormatException ignored) {
                    return;
                }
            });
            if (isOnline()) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        try {
            nibeUplinkRestApi.addTrackedParameter(systemId, Integer.parseInt(channelUID.getIdWithoutGroup()));
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        nibeUplinkRestApi.removeTrackedParameter(systemId, Integer.parseInt(channelUID.getId()));
    }


    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        super.handleConfigurationUpdate(configurationParameters);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing thinghandler for thing {}", thing.getUID().getAsString());
        nibeUplinkRestApi.removeCallbackListener(systemId);
    }

    /**
     * Checks whether the system is online
     *
     * @return false if the system is marked OFFLINE, otherwise true
     */
    public boolean isOnline() {
        ConnectionStatus status = nibeUplinkRestApi.getSystem(config.systemId).getConnectionStatus();
        return status != ConnectionStatus.OFFLINE;
    }

    /**
     * Gets information about the system configuration and software from Nibe uplink
     * and adds them as Thing properties
     */
    private void updateProperties() {
        SystemConfig systemConfig = nibeUplinkRestApi.getSystemConfig(systemId);
        SoftwareInfo softwareInfo = nibeUplinkRestApi.getSoftwareInfo(systemId);
        Map<String, String> properties = editProperties();
        properties.put(PROPERTY_HAS_COOLING, Boolean.toString(systemConfig.hasCooling()));
        properties.put(PROPERTY_HAS_HEATING, Boolean.toString(systemConfig.hasHeating()));
        properties.put(PROPERTY_HAS_HOT_WATER, Boolean.toString(systemConfig.hasHotWater()));
        properties.put(PROPERTY_HAS_VENTILATION, Boolean.toString(systemConfig.hasVentilation()));
        properties.put(PROPERTY_SOFTWARE_VERSION, softwareInfo.getCurrentVersion());
        updateProperties(properties);
    }

    @Override
    public void parametersUpdated(List<Parameter> parameterValues) {
        parameterValues.forEach(p -> {
            // Gets the full channel id (group#channel)
            String channelId = groupTypeProvider.getChannelFromID(p.getName());
            Channel channel = thing.getChannel(channelId);
            if (channel != null) {
                String itemType = channel.getAcceptedItemType();
                if (itemType == null) {
                    logger.warn("No item type defined for channel {}", channel.getUID());
                    return;
                }
                State state;
                switch (itemType) {
                    case CoreItemFactory.NUMBER:
                        // Nibe sends -32768 to mark a value as invalid
                        if (p.getRawValue() == -32768) {
                            state = UnDefType.UNDEF;
                            break;
                        }
                        String scalingFactor = "1";
                        // Check first channel configuration, then property to get scaling for the raw value
                        if (channel.getConfiguration().containsKey(CHANNEL_PROPERTY_SCALING_FACTOR)) {
                            scalingFactor = channel.getConfiguration().get(CHANNEL_PROPERTY_SCALING_FACTOR).toString();
                        } else {
                            scalingFactor = channel.getProperties().get(CHANNEL_PROPERTY_SCALING_FACTOR);
                        }
                        if (scalingFactor != null) {
                            try {
                                state = new DecimalType((double) p.getRawValue() / Integer.parseInt(scalingFactor));
                                break;
                            } catch (NumberFormatException ignored) {}
                        }
                        state = new DecimalType(p.getRawValue());
                        break;
                    case CoreItemFactory.SWITCH:
                        state = OnOffType.from(String.valueOf(p.getRawValue()));
                        break;
                    case CoreItemFactory.STRING:
                        state = new StringType(p.getDisplayValue());
                        break;
                    default:
                        state = UnDefType.UNDEF;
                }
                updateState(channelId, state);
            }
        });
    }

    @Override
    public void systemUpdated(NibeSystem system) {
        updateState(CHANNEL_LAST_ACTIVITY, new DateTimeType(system.getLastActivityDate()));
        updateState(CHANNEL_HAS_ALARMED, OnOffType.from(system.hasAlarmed()));
        if (system.getConnectionStatus() == ConnectionStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Nibe reports the system as offline");
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void softwareUpdateAvailable(SoftwareInfo softwareInfo) {
        if (softwareInfo.isUpgradeAvailable()) {
            updateState(CHANNEL_SOFTWARE_UPDATE, OnOffType.ON);
            updateState(CHANNEL_LATEST_SOFTWARE, new StringType(softwareInfo.getUpgradeAvailable().get("name")));
        } else {
            updateState(CHANNEL_SOFTWARE_UPDATE, OnOffType.OFF);
            updateState(CHANNEL_LATEST_SOFTWARE, new StringType(softwareInfo.getCurrentVersion()));
        }
    }

    @Override
    public void modeUpdated(Mode mode) {
        updateState(CHANNEL_MODE, new DecimalType(mode.asInt()));
    }
}
