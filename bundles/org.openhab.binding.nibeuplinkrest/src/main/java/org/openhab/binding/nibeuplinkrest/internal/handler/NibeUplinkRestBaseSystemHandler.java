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
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.*;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;


/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBaseSystemHandler extends BaseThingHandler {

    private @NonNullByDefault({}) NibeUplinkRestApi nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestBaseSystemConfiguration config;
    private @NonNullByDefault({}) int systemId;

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
        systemId = config.systemId;
        Bridge bridge = getBridge();
        if (bridge != null && bridge.getHandler() != null) {
            NibeUplinkRestBridgeHandler bridgeHandler = (NibeUplinkRestBridgeHandler) bridge.getHandler();
            nibeUplinkRestApi = bridgeHandler.getApiConnection();
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_UNINITIALIZED, "No bridge found");
        }
        scheduler.execute(() -> {
            updateProperties();
            addChannels();
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

    private void updateProperties() {
        SystemConfig systemConfig = nibeUplinkRestApi.getSystemConfig(systemId);
        SoftwareInfo softwareInfo = nibeUplinkRestApi.getSoftwareInfo(systemId);
        thing.setProperty(PROPERTY_HAS_COOLING, Boolean.toString(systemConfig.hasCooling()));
        thing.setProperty(PROPERTY_HAS_HEATING, Boolean.toString(systemConfig.hasHeating()));
        thing.setProperty(PROPERTY_HAS_HOT_WATER, Boolean.toString(systemConfig.hasHotWater()));
        thing.setProperty(PROPERTY_HAS_VENTILATION, Boolean.toString(systemConfig.hasVentilation()));
        thing.setProperty(PROPERTY_SOFTWARE_VERSION, softwareInfo.getCurrentVersion());
    }

    private void addChannels() {
        List<Category> categories = nibeUplinkRestApi.getCategories(config.systemId, true);
        List<Channel> channels = new ArrayList<>();
        for (Category category : categories) {
            if (!category.getCategoryId().equals("SYSTEM_INFO")) {
                ChannelGroupUID cgid = new ChannelGroupUID(thing.getUID(), category.getCategoryId());
                ChannelGroupTypeUID cgtid = new ChannelGroupTypeUID(cgid.getAsString());
                ChannelGroupType cgt = ChannelGroupTypeBuilder.instance(cgtid, category.getName()).build();
                ChannelGroupDefinition definition = new ChannelGroupDefinition(category.getCategoryId(), cgtid);
                for (Parameter parameter : category.getParameters()) {
                    ChannelUID cid = new ChannelUID(cgid, parameter.getName());
                    ChannelTypeUID ctid = new ChannelTypeUID(BINDING_ID, parameter.getName());
                    Channel channel = ChannelBuilder.create(cid, "Number")
                            .withLabel(parameter.getTitle()).withType(ctid).build();
                    channels.add(channel);
                }
            }
        }
        updateThing(editThing().withChannels(channels).build());
    }
}
