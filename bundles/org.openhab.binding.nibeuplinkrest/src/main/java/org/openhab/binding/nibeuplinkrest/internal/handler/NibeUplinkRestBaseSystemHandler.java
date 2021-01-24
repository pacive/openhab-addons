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

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import java.time.ZoneId;
import java.util.*;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.*;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.eclipse.smarthome.core.types.util.UnitUtils;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestCallbackListener;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestChannelGroupTypeProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        if (groupId == null) {
            logger.debug("Invalid channel");
            return;
        }
        if (command == RefreshType.REFRESH) {
            switch (channelId) {
                case CHANNEL_MODE_ID:
                    nibeUplinkRestApi.requestMode(systemId);
                    break;
                case CHANNEL_LAST_ACTIVITY_ID:
                case CHANNEL_HAS_ALARMED_ID:
                    nibeUplinkRestApi.requestSystem(systemId);
                    break;
                case CHANNEL_SOFTWARE_UPDATE_ID:
                case CHANNEL_LATEST_SOFTWARE_ID:
                    nibeUplinkRestApi.requestSoftwareInfo(systemId);
                    break;
                default:
                    try {
                        nibeUplinkRestApi.requestParameters(systemId,
                                Collections.singleton(Integer.parseInt(channelId)));
                    } catch (NumberFormatException e) {
                        logger.debug("Failed to parse channel id: {}", channelId);
                    }
            }
        } else if (groupId.equals(CHANNEL_GROUP_CONTROL_ID)) {
            // Only channels in the control channel group can be commanded
            if (channelId.equals(CHANNEL_MODE_ID) && command instanceof StringType) {
                nibeUplinkRestApi.setMode(systemId, Mode.from(command.toFullString()));
                // All channels except for mode accept numbers as command
            } else if (command instanceof DecimalType) {
                try {
                    Map<Integer, Integer> parameter = new HashMap<>();
                    Channel channel = thing.getChannel(channelUID);
                    if (channel == null) {
                        throw new NibeUplinkRestException("Channel doesn't exist");
                    }
                    int outValue = transformOutgoing(channel, (Number) command);
                    parameter.put(Integer.parseInt(channelId), outValue);
                    nibeUplinkRestApi.setParameters(systemId, parameter);
                } catch (Exception e) {
                    logger.warn("Failed to send command to channel {} with value {}. Reason: {}", channelId,
                            command.toFullString(), e.getMessage());
                }
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing thing {}", thing.getUID().getAsString());
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
            return;
        }
        scheduler.execute(() -> {
            nibeUplinkRestApi.requestSystem(systemId);
            thing.getChannels().stream().filter(c -> isLinked(c.getUID())).forEach(c -> {
                try {
                    nibeUplinkRestApi.addTrackedParameter(systemId, Integer.parseInt(c.getUID().getIdWithoutGroup()));
                } catch (NumberFormatException ignored) {
                }
            });
        });
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        try {
            nibeUplinkRestApi.addTrackedParameter(systemId, Integer.parseInt(channelUID.getIdWithoutGroup()));
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        if (!isLinked(channelUID)) {
            try {
                nibeUplinkRestApi.removeTrackedParameter(systemId, Integer.parseInt(channelUID.getIdWithoutGroup()));
            } catch (NumberFormatException ignored) {
            }
        }
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

    @Override
    public void parametersUpdated(List<Parameter> parameterValues) {
        logger.trace("Updating {} parameters for system {}", parameterValues.size(), this.systemId);
        parameterValues.forEach(p -> {
            // Gets the full channel id (group#channel)
            String channelId = groupTypeProvider.getChannelFromID(p.getName());
            @Nullable
            Channel channel = thing.getChannel(channelId);
            if (channel != null) {
                @Nullable
                String itemType = channel.getAcceptedItemType();
                if (itemType == null) {
                    logger.warn("No item type defined for channel {}", channel.getUID());
                    return;
                }
                State state;
                switch (itemType) {
                    case CoreItemFactory.SWITCH:
                        state = OnOffType.from(String.valueOf(p.getRawValue()));
                        break;
                    case CoreItemFactory.STRING:
                        state = new StringType(p.getDisplayValue());
                        break;
                    case CoreItemFactory.NUMBER:
                        state = new DecimalType(p.getRawValue());
                        break;
                    default:
                        if (itemType.startsWith(CoreItemFactory.NUMBER)) {
                            // Nibe sends -32768 to mark a value as invalid
                            if (p.getRawValue() == Short.MIN_VALUE) {
                                state = UnDefType.UNDEF;
                            } else {
                                state = transformIncoming(channel, p);
                            }
                        } else {
                            state = UnDefType.UNDEF;
                        }
                }
                logger.trace("Setting channel {} to {}", channel.getUID().getId(), state.toString());
                updateState(channelId, state);
            }
        });
    }

    @Override
    public void systemUpdated(NibeSystem system) {
        logger.trace("Updating system {}", this.systemId);
        updateState(CHANNEL_LAST_ACTIVITY,
                new DateTimeType(system.getLastActivityDate().withZoneSameInstant(ZoneId.systemDefault())));
        updateState(CHANNEL_HAS_ALARMED, OnOffType.from(system.hasAlarmed()));
        if (system.hasAlarmed()) {
            nibeUplinkRestApi.requestLatestAlarm(systemId);
        }
        if (system.getConnectionStatus() == ConnectionStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, "Nibe reports the system as offline");
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void statusUpdated(Set<String> activeComponents) {
        logger.trace("Updating status channels for system {}", this.systemId);
        thing.getChannelsOfGroup(CHANNEL_GROUP_STATUS_ID).stream()
                .filter(c -> Objects.equals(c.getAcceptedItemType(), CoreItemFactory.SWITCH)
                        && !c.getUID().getId().equals(CHANNEL_HAS_ALARMED)
                        && !c.getUID().getId().equals(CHANNEL_SOFTWARE_UPDATE))
                .forEach(channel -> {
                    if (activeComponents.contains(channel.getUID().getIdWithoutGroup())) {
                        updateState(channel.getUID(), OnOffType.ON);
                    } else {
                        updateState(channel.getUID(), OnOffType.OFF);
                    }
                });
    }

    @Override
    public void softwareUpdateAvailable(SoftwareInfo softwareInfo) {
        logger.trace("Updating software channels for system {}", this.systemId);
        if (softwareInfo.isUpgradeAvailable()) {
            updateState(CHANNEL_SOFTWARE_UPDATE, OnOffType.ON);
            updateState(CHANNEL_LATEST_SOFTWARE, new StringType(softwareInfo.getUpgradeAvailable()));
        } else {
            updateState(CHANNEL_SOFTWARE_UPDATE, OnOffType.OFF);
            updateState(CHANNEL_LATEST_SOFTWARE, new StringType(softwareInfo.getCurrentVersion()));
            if (!softwareInfo.getCurrentVersion().equals(thing.getProperties().get(PROPERTY_SOFTWARE_VERSION))) {
                Map<String, String> properties = editProperties();
                properties.put(PROPERTY_SOFTWARE_VERSION, softwareInfo.getCurrentVersion());
                updateProperties(properties);
            }
        }
    }

    @Override
    public void modeUpdated(Mode mode) {
        logger.trace("Updating mode for system {}", this.systemId);
        updateState(CHANNEL_MODE, new StringType(mode.toString()));
    }

    @Override
    public void alarmInfoUpdated(AlarmInfo alarmInfo) {
        logger.trace("Updating alarm info for system {}", this.systemId);
        updateState(CHANNEL_ALARM_INFO, new StringType(alarmInfo.toString()));
    }

    private State transformIncoming(Channel channel, Parameter parameter) {
        @Nullable
        Unit<?> unit = UnitUtils.parseUnit(parameter.getUnit());
        int scalingFactor = getScalingFactor(channel);

        double value = (double) parameter.getRawValue() / scalingFactor;

        if (unit == null) {
            return new DecimalType(value);
        } else {
            return new QuantityType<>(value, unit);
        }
    }

    private int transformOutgoing(Channel channel, Number outgoingValue) {
        double transformed = outgoingValue.doubleValue();
        int scalingFactor = getScalingFactor(channel);

        return (int) transformed * scalingFactor;
    }

    private int getScalingFactor(Channel channel) {
        String scalingFactor;
        // Check first channel configuration, then property to get scaling factor
        if (channel.getConfiguration().containsKey(CHANNEL_PROPERTY_SCALING_FACTOR)) {
            scalingFactor = channel.getConfiguration().get(CHANNEL_PROPERTY_SCALING_FACTOR).toString();
        } else {
            scalingFactor = channel.getProperties().getOrDefault(CHANNEL_PROPERTY_SCALING_FACTOR, "1");
        }
        try {
            return Integer.parseInt(scalingFactor);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
