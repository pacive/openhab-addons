/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.mitsubishiheatpump.internal;

import static org.openhab.binding.mitsubishiheatpump.internal.MitsubishiHeatpumpBindingConstants.*;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.mitsubishiheatpump.internal.exception.ConfigurationException;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.http.HttpAPI;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.GenericPacket;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.GetSettingsCommand;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.GetTemperaturesCommand;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.MitsubishiHeatpumpCommand;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.MitsubishiHeatpumpPDU;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.SetRemoteTemperatureCommand;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.SetRequestPacket;
import org.openhab.binding.mitsubishiheatpump.internal.util.CryptoHelper;
import org.openhab.binding.mitsubishiheatpump.internal.util.ThingHandlerHelper;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MitsubishiHeatpumpHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class MitsubishiHeatpumpHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MitsubishiHeatpumpHandler.class);

    private final HttpAPI api;
    @Nullable
    Future<?> pollingThread;

    public MitsubishiHeatpumpHandler(Thing thing, HttpClient httpClient, CryptoHelper cryptoHelper) {
        super(thing);
        this.api = new HttpAPI(httpClient, cryptoHelper, this);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            sendPacket(ThingHandlerHelper.createGetSettingsPDU());
            return;
        }
        try {
            switch (channelUID.getId()) {
                case CHANNEL_POWER:
                    sendPacket(ThingHandlerHelper.createSetPowerPDU(command));
                    break;
                case CHANNEL_MODE:
                    sendPacket(ThingHandlerHelper.createSetModePDU(command));
                    break;
                case CHANNEL_SETPOINT:
                    sendPacket(ThingHandlerHelper.createSetpointPDU(command));
                    break;
                case CHANNEL_FAN:
                    sendPacket(ThingHandlerHelper.createSetFanPDU(command));
                    break;
                case CHANNEL_VERTICAL_VANE:
                    sendPacket(ThingHandlerHelper.createSetVerticalVanePDU(command));
                    break;
                case CHANNEL_HORIZONTAL_VANE:
                    sendPacket(ThingHandlerHelper.createSetHorizontalVanePDU(command));
                    break;
                case CHANNEL_REMOTE_TEMP:
                    sendPacket(ThingHandlerHelper.createSetRemoteTempPDU(command));
                    break;
            }
            scheduler.schedule(() -> sendPacket(ThingHandlerHelper.createGetSettingsPDU()), 5, TimeUnit.SECONDS);
        } catch (SerialProtocolException e) {
            logger.warn("Failed to send command {}", e.getMessage());
            logger.debug("", e);
        }
    }

    @Override
    public void initialize() {
        MitsubishiHeatpumpConfiguration conf = getConfigAs(MitsubishiHeatpumpConfiguration.class);
        try {
            api.setHost(conf.hostname);
        } catch (URISyntaxException e) {
            logger.error("Invalid hostname");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid hostname");
            return;
        }
        scheduler.execute(this::getStatus);
        startPolling();
    }

    @Override
    public void dispose() {
        super.dispose();
        stopPolling();
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        super.channelUnlinked(channelUID);
        if (CHANNEL_REMOTE_TEMP.equals(channelUID.getId())) {
            SetRemoteTemperatureCommand mc = new SetRemoteTemperatureCommand();
            sendPacket(new SetRequestPacket(mc));
        }
    }

    private void startPolling() {
        MitsubishiHeatpumpConfiguration conf = getConfigAs(MitsubishiHeatpumpConfiguration.class);
        logger.debug("Start polling every {} seconds", conf.refreshInterval);
        Future<?> localPollingThread = this.pollingThread;
        if (localPollingThread == null || localPollingThread.isCancelled() || localPollingThread.isDone()) {
            this.pollingThread = scheduler.scheduleWithFixedDelay(this::getStatus, conf.refreshInterval,
                    conf.refreshInterval, TimeUnit.SECONDS);
        }
    }

    private void stopPolling() {
        Future<?> localPollingThread = this.pollingThread;
        if (localPollingThread != null) {
            localPollingThread.cancel(true);
            this.pollingThread = null;
        }
    }

    private void getStatus() {
        try {
            api.requestStatus();
        } catch (ConfigurationException e) {
            logger.warn("{}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    private void sendPacket(MitsubishiHeatpumpPDU pdu) {
        try {
            api.sendPacket(pdu);
        } catch (ConfigurationException e) {
            logger.warn("{}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR);
        }
    }

    public synchronized void onDeviceInfoResponse(Map<String, String> deviceInfo) {
        updateProperties(deviceInfo);
        updateStatus(ThingStatus.ONLINE);
    }

    public synchronized void onPDU(MitsubishiHeatpumpPDU pdu) {
        MitsubishiHeatpumpCommand command = pdu.getCommand();
        logger.trace("Received {}", pdu);
        if (pdu instanceof GenericPacket) {
            return;
        }

        switch (command) {
            case GetSettingsCommand gsc -> {
                updateState(CHANNEL_POWER, gsc.getPower().asState());
                updateState(CHANNEL_MODE, gsc.getOperatingMode().asState());
                updateState(CHANNEL_SETPOINT, gsc.getTargetTemperature().asState());
                updateState(CHANNEL_FAN, gsc.getFanMode().asState());
                updateState(CHANNEL_VERTICAL_VANE, gsc.getVerticalVane().asState());
                updateState(CHANNEL_HORIZONTAL_VANE, gsc.getHorizontalVane().asState());
            }
            case GetTemperaturesCommand gtc -> {
                updateState(CHANNEL_OUTDOOR_TEMP, gtc.getOutdoorUnitTemperature().asState());
                updateState(CHANNEL_INDOOR_TEMP, gtc.getCurrentTemperature().asState());
                updateState(CHANNEL_RUNTIME, gtc.getRuntime().asState());
            }
            default -> {
            }
        }
    }
}
