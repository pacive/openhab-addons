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

package org.openhab.binding.nibeuplinkrest.internal.discovery;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBridgeHandler;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestDiscoveryService extends AbstractDiscoveryService
        implements DiscoveryService, ThingHandlerService {

    private final Logger logger = LoggerFactory.getLogger(NibeUplinkRestDiscoveryService.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_SYSTEM);
    private static final int TIMEOUT = 10;

    private @NonNullByDefault({}) NibeUplinkRestBridgeHandler bridgeHandler;

    public NibeUplinkRestDiscoveryService() {
        super(SUPPORTED_THING_TYPES, TIMEOUT);
    }

    @Override
    public void activate() {
        super.activate(new HashMap<String, @Nullable Object>());
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected void startScan() {
            scheduler.execute(this::findSystems);
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof NibeUplinkRestBridgeHandler) {
            bridgeHandler = (NibeUplinkRestBridgeHandler) handler;
        }

    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    private void findSystems() {
        if (bridgeHandler.getThing().getStatus() == ThingStatus.ONLINE) {
            logger.debug("Starting discovery");
            NibeUplinkRestApi connection = bridgeHandler.getApiConnection();
            connection.getConnectedSystems().forEach(this::thingDiscovered);
        }
    }

    private void thingDiscovered(NibeSystem system) {
        logger.debug("Found system with id {}", system.getSystemId());
        Map<String, Object> properties = new HashMap<>();
        final ThingUID bridgeUID = bridgeHandler.getThing().getUID();

        properties.put(PROPERTY_SYSTEM_ID, system.getSystemId());
        properties.put(PROPERTY_NAME, system.getName());
        properties.put(PROPERTY_PRODUCT_NAME, system.getProductName());
        properties.put(PROPERTY_SECURITY_LEVEL, system.getSecurityLevel());
        properties.put(PROPERTY_SERIAL_NUMBER, system.getSerialNumber());

        ThingUID thingUID = new ThingUID(THING_TYPE_SYSTEM, bridgeUID,
                Integer.toString(system.getSystemId()));

        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withBridge(bridgeUID)
                .withProperties(properties).withRepresentationProperty(PROPERTY_SYSTEM_ID)
                .withLabel(system.getName()).build();

        thingDiscovered(result);
    }
}
