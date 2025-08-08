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
package org.openhab.binding.zaptec.internal.discovery;

import static org.openhab.binding.zaptec.internal.ZaptecBindingConstants.THING_TYPE_CHARGER;
import static org.openhab.binding.zaptec.internal.ZaptecBindingConstants.THING_TYPE_INSTALLATION;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.zaptec.internal.api.ZaptecApi;
import org.openhab.binding.zaptec.internal.handler.ZaptecAccountHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * The {@link ZaptecDiscoveryService} class.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class ZaptecDiscoveryService extends AbstractDiscoveryService implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(ZaptecDiscoveryService.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Set.of(THING_TYPE_CHARGER, THING_TYPE_INSTALLATION);
    private static final int TIMEOUT = 10;

    private @NonNullByDefault({}) ZaptecAccountHandler accountHandler;

    public ZaptecDiscoveryService() throws IllegalArgumentException {
        super(SUPPORTED_THING_TYPES, TIMEOUT);
    }

    @Override
    protected void startScan() {
        ZaptecApi api = accountHandler.getApi();
        api.getChargers(this::onChargersResponse, this::onError);
    }

    @Override
    public void setThingHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof ZaptecAccountHandler) {
            accountHandler = (ZaptecAccountHandler) thingHandler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return accountHandler;
    }

    @Override
    public void activate() {
        super.activate(Map.of());
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    public void onChargersResponse(JsonElement result) {
        logger.debug("Response: {}", result);
    }

    public void onError(Integer code, String content) {
        logger.warn("Error retrieving discover data. Errorcode: {}, message: {}", code, content);
    }
}
