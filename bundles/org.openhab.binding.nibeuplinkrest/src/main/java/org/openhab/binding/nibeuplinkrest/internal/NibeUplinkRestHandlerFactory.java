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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.nibeuplinkrest.internal.auth.NibeUplinkRestOAuthService;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBaseSystemHandler;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestThermostatHandler;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestTypeFactory;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
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

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_APIBRIDGE, THING_TYPE_SYSTEM,
            THING_TYPE_THERMOSTAT);

    private @NonNullByDefault({}) OAuthFactory oAuthFactory;
    private @NonNullByDefault({}) NibeUplinkRestOAuthService oAuthService;
    private @NonNullByDefault({}) HttpClient httpClient;
    private @NonNullByDefault({}) NibeUplinkRestTypeFactory typeFactory;
    private @NonNullByDefault({}) ItemChannelLinkRegistry itemChannelLinkRegistry;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID) || thingTypeUID.getBindingId().equals(BINDING_ID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_APIBRIDGE.equals(thingTypeUID)) {
            final NibeUplinkRestBridgeHandler handler = new NibeUplinkRestBridgeHandler((Bridge) thing, oAuthFactory,
                    httpClient, typeFactory);
            oAuthService.addBridgeHandler(handler);
            return handler;
        }
        if (THING_TYPE_THERMOSTAT.equals(thingTypeUID)) {
            return new NibeUplinkRestThermostatHandler(thing, itemChannelLinkRegistry);
        }
        if (thingTypeUID.getBindingId().equals(BINDING_ID)) {
            return new NibeUplinkRestBaseSystemHandler(thing, typeFactory.getGroupTypeProvider());
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
    protected void setOAuthService(NibeUplinkRestOAuthService oAuthService) {
        this.oAuthService = oAuthService;
    }

    protected void unsetOAuthService(NibeUplinkRestOAuthService oAuthService) {
        this.oAuthService = null;
    }

    @Reference
    protected void setTypeFactory(NibeUplinkRestTypeFactory typeFactory) {
        this.typeFactory = typeFactory;
    }

    protected void unsetTypeFactory(NibeUplinkRestTypeFactory typeFactory) {
        this.typeFactory = null;
    }

    @Reference
    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry itemChannelLinkRegistry) {
        this.itemChannelLinkRegistry = null;
    }
}
