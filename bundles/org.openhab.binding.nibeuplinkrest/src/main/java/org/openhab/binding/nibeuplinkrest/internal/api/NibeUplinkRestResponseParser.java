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

package org.openhab.binding.nibeuplinkrest.internal.api;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.nibeuplinkrest.internal.api.model.ConnectionStatus;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.SoftwareInfo;
import org.openhab.binding.nibeuplinkrest.internal.api.model.SystemConfig;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestResponseParser {

    private static final JsonParser parser = new JsonParser();

    private static final Logger logger = LoggerFactory.getLogger(NibeUplinkRestResponseParser.class);

    public static NibeSystem parseSystem(JsonElement tree) throws NibeUplinkParseException {
        int systemId = 0;
        String name = "";
        String productName = "";
        String securityLevel = "";
        String serialNumber = "";
        ZonedDateTime lastActivityDate = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;
        boolean hasAlarmed = false;

        if (tree.isJsonObject()) {
            final JsonObject systemObject = tree.getAsJsonObject();
            try {
                systemId = systemObject.get(PROPERTY_SYSTEM_ID).getAsInt();
                name = systemObject.get(PROPERTY_NAME).getAsString();
                productName = systemObject.get(PROPERTY_PRODUCT_NAME).getAsString();
                securityLevel = systemObject.get(PROPERTY_SECURITY_LEVEL).getAsString();
                serialNumber = systemObject.get(PROPERTY_SERIAL_NUMBER).getAsString();
                lastActivityDate = ZonedDateTime.parse(systemObject.get(SYSTEM_LAST_ACTIVITY).getAsString());
                connectionStatus = ConnectionStatus.from(systemObject.get(SYSTEM_CONNECTION_STATUS).getAsString());
                hasAlarmed = systemObject.get(SYSTEM_HAS_ALARMED).getAsBoolean();
            } catch (RuntimeException e) {
                logger.warn("Error parsing system: ", e);
            }
        }
        if (systemId == 0) {
            throw new NibeUplinkParseException("Error parsing system.");
        }
        return new NibeSystem(systemId, name, productName, securityLevel, serialNumber, lastActivityDate,
                connectionStatus, hasAlarmed);
    }

    public static NibeSystem parseSystem(String json) {
        final JsonElement tree = parser.parse(json);
        return parseSystem(tree);
    }

    public static List<NibeSystem> parseSystemList(String json) {
        final List<NibeSystem> systems = new ArrayList<>();
        final JsonObject tree = (JsonObject) parser.parse(json);
        JsonArray arr = tree.get("objects").getAsJsonArray();
        for (JsonElement e : arr) {
            systems.add(parseSystem(e));
        }
        return systems;
    }

    public static SystemConfig parseSystemConfig(String json) {
        boolean hasCooling = false;
        boolean hasHeating = false;
        boolean hasHotWater = false;
        boolean hasVentilation = false;

        final JsonObject tree = (JsonObject) parser.parse(json);
        try {
            hasCooling = tree.get(PROPERTY_HAS_COOLING).getAsBoolean();
            hasHeating = tree.get(PROPERTY_HAS_HEATING).getAsBoolean();
            hasHotWater = tree.get(PROPERTY_HAS_HOT_WATER).getAsBoolean();
            hasVentilation = tree.get(PROPERTY_HAS_VENTILATION).getAsBoolean();
        } catch (RuntimeException e) {
            logger.warn("Error parsing system config: ", e);
        }
        return new SystemConfig(hasCooling, hasHeating, hasHotWater, hasVentilation);
    }

    public static SoftwareInfo parseSoftwareInfo(String json) {
        String currentVersion = "";
        String upgradeAvailable = null;

        final JsonObject tree = (JsonObject) parser.parse(json);
        try {
            currentVersion = tree.get("current").getAsJsonObject().get("name").getAsString();
            if(tree.has("upgrade") && !tree.get("upgrade").isJsonNull()) {
                upgradeAvailable = tree.get("upgrade").getAsJsonObject().get("name").getAsString();
            }
        } catch (RuntimeException e) {
            logger.warn("Error parsing system config: ", e);
        }
        return new SoftwareInfo(currentVersion, upgradeAvailable);
    }
}
