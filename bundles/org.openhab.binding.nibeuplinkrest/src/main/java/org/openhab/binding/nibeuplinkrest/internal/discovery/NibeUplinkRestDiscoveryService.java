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
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestTypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

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
    private @NonNullByDefault({}) NibeUplinkRestTypeFactory typeFactory;

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
        if (bridgeHandler.getThing().getStatus() == ThingStatus.ONLINE) {
            logger.debug("Starting discovery");
            NibeUplinkRestApi connection = bridgeHandler.getApiConnection();
            connection.getConnectedSystems().forEach(system -> {
                logger.debug("Found system with id {}", system.getSystemId());
                try {
                    if (!system.isConfigSet()) {
                        system.setConfig(connection.getSystemConfig(system.getSystemId()));
                    }
                    List<Category> categories = connection.getCategories(system.getSystemId(), true);
                    thingDiscovered(system, categories);
                } catch (NibeUplinkRestException e) {
                    logger.warn("Error retrieving properties for system {}", system.getSystemId());
                }
            });
        }
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof NibeUplinkRestBridgeHandler) {
            bridgeHandler = (NibeUplinkRestBridgeHandler) handler;
            typeFactory = bridgeHandler.getTypeFactory();
        }

    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }

    /**
     * Prepares a thing for adding as a discovery result, and creates a thing-type
     * if it doesn't exist
     *
     * @param system
     * @param categories
     */
    private void thingDiscovered(NibeSystem system, List<Category> categories) {
        Map<String, Object> properties = new HashMap<>();
        final ThingUID bridgeUID = bridgeHandler.getThing().getUID();

        properties.put(PROPERTY_SYSTEM_ID, system.getSystemId());
        properties.put(PROPERTY_NAME, system.getName());
        properties.put(PROPERTY_PRODUCT_NAME, system.getProductName());
        properties.put(PROPERTY_SECURITY_LEVEL, system.getSecurityLevel());
        properties.put(PROPERTY_SERIAL_NUMBER, system.getSerialNumber());
        properties.put(PROPERTY_HAS_COOLING, system.hasCooling());
        properties.put(PROPERTY_HAS_HEATING, system.hasHeating());
        properties.put(PROPERTY_HAS_HOT_WATER, system.hasHotWater());
        properties.put(PROPERTY_HAS_VENTILATION, system.hasVentilation());

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID,
                system.getProductName().replaceAll("\\s", "").toLowerCase(Locale.ROOT));

        if (!typeFactory.hasThingType(thingTypeUID)) {
            typeFactory.createThingType(system, categories);
        }

        ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID,
                Integer.toString(system.getSystemId()));

        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID)
                .withLabel(system.getName())
                .withBridge(bridgeUID)
                .withProperties(properties)
                .withRepresentationProperty(PROPERTY_SYSTEM_ID)
                .build();

        thingDiscovered(result);
    }
}
