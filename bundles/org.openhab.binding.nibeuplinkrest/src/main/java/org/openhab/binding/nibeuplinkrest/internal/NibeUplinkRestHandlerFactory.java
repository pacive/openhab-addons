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

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.auth.client.oauth2.OAuthFactory;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.HttpClientFactory;
import org.openhab.binding.nibeuplinkrest.internal.auth.NibeUplinkRestOAuthService;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBaseSystemHandler;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestThermostatHandler;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestChannelGroupTypeProvider;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestChannelTypeProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link NibeUplinkRestHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.nibeuplinkrest")
public class NibeUplinkRestHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(THING_TYPE_APIBRIDGE, THING_TYPE_SYSTEM, THING_TYPE_THERMOSTAT)
                    .collect(Collectors.toSet()));

    private @NonNullByDefault({}) NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider;
    private @NonNullByDefault({}) NibeUplinkRestChannelTypeProvider channelTypeProvider;
    private @NonNullByDefault({}) OAuthFactory oAuthFactory;
    private @NonNullByDefault({}) NibeUplinkRestOAuthService oAuthService;
    private @NonNullByDefault({}) HttpClient httpClient;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_APIBRIDGE.equals(thingTypeUID)) {
            final NibeUplinkRestBridgeHandler handler = new NibeUplinkRestBridgeHandler
                    ((Bridge) thing, oAuthFactory, httpClient);
            oAuthService.addBridgeHandler(handler);
            return handler;
        }
        if (THING_TYPE_SYSTEM.equals(thingTypeUID)) {
            final NibeUplinkRestBaseSystemHandler handler = new NibeUplinkRestBaseSystemHandler(thing,
                    channelGroupTypeProvider, channelTypeProvider);
            return handler;
        }
        if (THING_TYPE_THERMOSTAT.equals(thingTypeUID)) {
            final NibeUplinkRestThermostatHandler handler = new NibeUplinkRestThermostatHandler(thing);
            return handler;
        }

        return null;
    }

    @Reference
    protected void setOAuthFactory(OAuthFactory oAuthFactory) {
        this.oAuthFactory = oAuthFactory;
    }

    protected void unsetOAuthFactory(OAuthFactory oAuthFactory) {
        this.oAuthFactory = null;
    }

    @Reference
    protected void setHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    protected void unsetHttpClientFactory(HttpClientFactory httpClientFactory) {
        this.httpClient = null;
    }

    @Reference
    protected void setOAuthService(NibeUplinkRestOAuthService oAuthService) { this.oAuthService = oAuthService; }

    protected void unsetOAuthService(NibeUplinkRestOAuthService oAuthService) { this.oAuthService = null; }

    @Reference
    protected void setChannelGroupTypeProvider(NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProvider = channelGroupTypeProvider;
    }

    protected void unsetChannelGroupTypeProvider(NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProvider = null;
    }

    @Reference
    protected void setChannelTypeProvider(NibeUplinkRestChannelTypeProvider channelTypeProvider) {
        this.channelTypeProvider = channelTypeProvider;
    }

    protected void unsetChannelTypeProvider(NibeUplinkRestChannelTypeProvider channelTypeProvider) {
        this.channelTypeProvider = null;
    }
}
