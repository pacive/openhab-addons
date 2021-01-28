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

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import java.util.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.openhab.binding.nibeuplinkrest.internal.api.NibeUplinkRestApi;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestException;
import org.openhab.binding.nibeuplinkrest.internal.handler.NibeUplinkRestBridgeHandler;
import org.openhab.binding.nibeuplinkrest.internal.provider.NibeUplinkRestTypeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        ThingUID thingUID = new ThingUID(thingTypeUID, bridgeUID, Integer.toString(system.getSystemId()));

        DiscoveryResult result = DiscoveryResultBuilder.create(thingUID).withLabel(system.getName())
                .withBridge(bridgeUID).withProperties(properties).withRepresentationProperty(PROPERTY_SYSTEM_ID)
                .build();

        thingDiscovered(result);

        categories.stream()
                .filter((category) -> (category.getCategoryId().startsWith("SYSTEM")
                        && !category.getCategoryId().equals("SYSTEM_INFO")))
                .map((category) -> Integer.parseInt(category.getCategoryId().substring(7)))
                .forEach(i -> thingDiscovered(createThermostatResult(bridgeUID, system.getSystemId(), i)));
    }

    private DiscoveryResult createThermostatResult(ThingUID bridgeUID, int systemId, int climateSystemNo) {
        String thingID = systemId + "-thermostat-" + climateSystemNo;
        ThingUID thingUID = new ThingUID(THING_TYPE_THERMOSTAT, bridgeUID, thingID);
        String name = "System " + climateSystemNo + " Thermostat";

        Map<String, Object> properties = new HashMap<>();
        properties.put(PROPERTY_ID, climateSystemNo);
        properties.put(PROPERTY_SYSTEM_ID, systemId);
        properties.put(PROPERTY_NAME, name);
        properties.put(PROPERTY_CLIMATE_SYSTEMS, Collections.singletonList(climateSystemNo));

        return DiscoveryResultBuilder.create(thingUID).withBridge(bridgeUID).withLabel(name).withProperties(properties)
                .withRepresentationProperty(PROPERTY_ID).build();
    }
}
