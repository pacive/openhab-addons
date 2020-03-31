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
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.*;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestCallbackListener;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestChannelGroupTypeProvider;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestChannelTypeProvider;
import org.openhab.binding.nibeuplinkrest.internal.util.StringConvert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;


/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBaseSystemHandler extends BaseThingHandler implements NibeUplinkRestCallbackListener {

    private final NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider;
    private final NibeUplinkRestChannelTypeProvider channelTypeProvider;
    private @NonNullByDefault({}) NibeUplinkRestApi nibeUplinkRestApi;
    private @NonNullByDefault({}) NibeUplinkRestBaseSystemConfiguration config;
    private @NonNullByDefault({}) int systemId;

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestBaseSystemHandler.class);

    public NibeUplinkRestBaseSystemHandler(Thing thing, NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider,
                                           NibeUplinkRestChannelTypeProvider channelTypeProvider) {
        super(thing);
        this.channelGroupTypeProvider = channelGroupTypeProvider;
        this.channelTypeProvider = channelTypeProvider;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        //TODO: handle commands
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
            addChannels();
            thing.getChannels().stream().filter(c -> isLinked(c.getUID())).forEach(c -> {
                nibeUplinkRestApi.addTrackedParameter(systemId, Integer.parseInt(c.getUID().getIdWithoutGroup()));
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
        super.channelLinked(channelUID);
        nibeUplinkRestApi.addTrackedParameter(systemId, Integer.parseInt(channelUID.getIdWithoutGroup()));
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        super.channelUnlinked(channelUID);
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
        channels.addAll(thing.getChannels());
        for (Category category : categories) {
            if (!category.getCategoryId().equals("SYSTEM_INFO")) {
                String cg = StringConvert.snakeCaseToCamelCase(category.getCategoryId());
                ChannelGroupTypeUID cgtid = new ChannelGroupTypeUID(BINDING_ID, cg);
                ChannelGroupUID cgid = new ChannelGroupUID(thing.getUID(), cg);
                List<ChannelDefinition> channelDefinitions = new ArrayList<>();
                for (Parameter parameter : category.getParameters()) {
                    ChannelTypeUID ctid = new ChannelTypeUID(BINDING_ID, parameter.getName());
                    ChannelType ct = ChannelTypeBuilder.state(ctid, parameter.getTitle(), "Number").build();
                    ChannelDefinition cd = new ChannelDefinitionBuilder(parameter.getName(), ctid)
                            .withLabel(parameter.getTitle()).build();
                    ChannelUID cid = new ChannelUID(cgid, parameter.getName());
                    Channel channel = ChannelBuilder.create(cid, "Number").withType(ctid)
                            .withLabel(parameter.getTitle()).build();
                    channelTypeProvider.add(ctid, ct);
                    channelDefinitions.add(cd);
                    channels.add(channel);
                }
                ChannelGroupType cgt = ChannelGroupTypeBuilder.instance(cgtid, category.getName())
                        .withChannelDefinitions(channelDefinitions).build();
                channelGroupTypeProvider.add(cgtid, cgt);
            }
        }
        updateThing(editThing().withChannels(channels).build());
    }

    @Override
    public void parametersUpdated(List<Parameter> parameterValues) {
        parameterValues.forEach(p -> {
            updateState("STATUS#" + p.getName(), new DecimalType(p.getRawValue()));
        });
    }

    @Override
    public void systemUpdated(NibeSystem system) {

    }

    @Override
    public void softwareUpdateAvailable(SoftwareInfo softwareInfo) {

    }

    @Override
    public void modeUpdated(Mode mode) {

    }
}
