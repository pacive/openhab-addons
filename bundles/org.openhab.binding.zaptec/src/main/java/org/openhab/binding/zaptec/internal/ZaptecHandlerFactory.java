/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.zaptec.internal;

import static org.openhab.binding.zaptec.internal.ZaptecBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.zaptec.internal.handler.ZaptecAccountHandler;
import org.openhab.binding.zaptec.internal.handler.ZaptecChargerHandler;
import org.openhab.binding.zaptec.internal.handler.ZaptecInstallationHandler;
import org.openhab.core.auth.client.oauth2.OAuthFactory;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ZaptecHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.zaptec", service = ThingHandlerFactory.class)
public class ZaptecHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_ACCOUNT,
            THING_TYPE_INSTALLATION, THING_TYPE_CHARGER);

    private final HttpClient httpClient;
    private final OAuthFactory oAuthFactory;

    @Activate
    public ZaptecHandlerFactory(@Reference HttpClientFactory httpClientFactory, @Reference OAuthFactory oAuthFactory) {
        super();
        this.httpClient = httpClientFactory.getCommonHttpClient();
        this.oAuthFactory = oAuthFactory;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_ACCOUNT.equals(thingTypeUID)) {
            return new ZaptecAccountHandler((Bridge) thing, httpClient, oAuthFactory);
        }
        if (THING_TYPE_INSTALLATION.equals(thingTypeUID)) {
            return new ZaptecInstallationHandler(thing);
        }
        if (THING_TYPE_CHARGER.equals(thingTypeUID)) {
            return new ZaptecChargerHandler(thing);
        }

        return null;
    }
}
