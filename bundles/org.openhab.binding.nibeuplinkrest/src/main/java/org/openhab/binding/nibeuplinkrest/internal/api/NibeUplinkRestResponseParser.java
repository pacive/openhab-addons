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
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;
import org.openhab.binding.nibeuplinkrest.internal.exception.NibeUplinkRestParseException;

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

    private static JsonParser parser = new JsonParser();

    public static NibeSystem parseSystem(JsonObject tree) throws NibeUplinkRestParseException {
        int systemId = 0;
        String name = "";
        String productName = "";
        String securityLevel = "";
        String serialNumber = "";
        ZonedDateTime lastActivityDate = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        ConnectionStatus connectionStatus = ConnectionStatus.OFFLINE;
        boolean hasAlarmed = false;

       try {
            systemId = tree.get(PROPERTY_SYSTEM_ID).getAsInt();
            name = tree.get(PROPERTY_NAME).getAsString();
            productName = tree.get(PROPERTY_PRODUCT_NAME).getAsString();
            securityLevel = tree.get(PROPERTY_SECURITY_LEVEL).getAsString();
            serialNumber = tree.get(PROPERTY_SERIAL_NUMBER).getAsString();
            lastActivityDate = ZonedDateTime.parse(tree.get(SYSTEM_LAST_ACTIVITY).getAsString());
            connectionStatus = ConnectionStatus.from(tree.get(SYSTEM_CONNECTION_STATUS).getAsString());
            hasAlarmed = tree.get(SYSTEM_HAS_ALARMED).getAsBoolean();
        } catch (RuntimeException e) {
           throw new NibeUplinkRestParseException("Error parsing system: ", e);
        }

        if (systemId == 0) {
            throw new NibeUplinkRestParseException("Error parsing system.");
        }
        return new NibeSystem(systemId, name, productName, securityLevel, serialNumber, lastActivityDate,
                connectionStatus, hasAlarmed);
    }

    public static NibeSystem parseSystem(String json) {
        return parseSystem(parser.parse(json).getAsJsonObject());
    }

    public static List<NibeSystem> parseSystemList(String json) {
        final List<NibeSystem> systems = new ArrayList<>();
        final JsonObject tree = parser.parse(json).getAsJsonObject();
        JsonArray arr = tree.get("objects").getAsJsonArray();
        for (JsonElement e : arr) {
            try {
                systems.add(parseSystem(e.getAsJsonObject()));
            } catch (NibeUplinkRestParseException ignored) {
                continue;
            }
        }
        return systems;
    }

    public static SystemConfig parseSystemConfig(String json) {
        boolean hasCooling = false;
        boolean hasHeating = false;
        boolean hasHotWater = false;
        boolean hasVentilation = false;

        final JsonObject tree = parser.parse(json).getAsJsonObject();
        try {
            hasCooling = tree.get(PROPERTY_HAS_COOLING).getAsBoolean();
            hasHeating = tree.get(PROPERTY_HAS_HEATING).getAsBoolean();
            hasHotWater = tree.get(PROPERTY_HAS_HOT_WATER).getAsBoolean();
            hasVentilation = tree.get(PROPERTY_HAS_VENTILATION).getAsBoolean();
        } catch (RuntimeException e) {
            throw new NibeUplinkRestParseException("Error parsing system config", e);
        }
        return new SystemConfig(hasCooling, hasHeating, hasHotWater, hasVentilation);
    }

    public static SoftwareInfo parseSoftwareInfo(String json) {
        String currentVersion = "";
        String upgradeAvailable = null;

        final JsonObject tree = (JsonObject) parser.parse(json);
        try {
            currentVersion = tree.get(SOFTWARE_CURRENT).getAsJsonObject().get(SOFTWARE_NAME).getAsString();
            if(tree.has(SOFTWARE_UPGRADE) && !tree.get(SOFTWARE_UPGRADE).isJsonNull()) {
                upgradeAvailable = tree.get(SOFTWARE_UPGRADE).getAsJsonObject().get(SOFTWARE_NAME).getAsString();
            }
        } catch (RuntimeException e) {
            throw new NibeUplinkRestParseException("Error parsing software info: ", e);
        }
        return new SoftwareInfo(currentVersion, upgradeAvailable);
    }

    public static List<Category> parseCategoryList(String json) {
        final List<Category> categories = new ArrayList<>();
        final JsonArray tree = parser.parse(json).getAsJsonArray();
            for (JsonElement e : tree) {
                try {
                    categories.add(parseCategory(e.getAsJsonObject()));
                } catch (NibeUplinkRestParseException ignored) {
                    continue;
                }
            }
        return categories;
    }

    public static Category parseCategory(JsonObject tree) {
        String categoryId = "";
        String name = "";
        List<Parameter> parameters;
        try {
            categoryId = tree.get("categoryId").getAsString();
            name = tree.get("name").getAsString();
            JsonArray parameterArray = tree.get("parameters").getAsJsonArray();
            parameters = parseParameterList(parameterArray);
        } catch (RuntimeException e) {
            throw new NibeUplinkRestParseException("Error parsing category: ", e);
        }
        return new Category(categoryId, name, parameters);
    }

    public static List<Parameter> parseParameterList(String json) {
        return parseParameterList(parser.parse(json).getAsJsonArray());
    }

    public static List<Parameter> parseParameterList(JsonArray tree) {
        List<Parameter> parameters = new ArrayList<>();
        for (JsonElement e : tree) {
            try {
                parameters.add(parseParameter(e.getAsJsonObject()));
            } catch (NibeUplinkRestParseException ignored) {
                continue;
            }
        }
        return parameters;
    }

    public static Parameter parseParameter(JsonObject tree) {
        int parameterId = 0;
        String name = "";
        String title = "";
        String designation = "";
        String unit = "";
        String displayValue = "";
        int rawValue = 0;

        try {
            parameterId = tree.get("parameterId").getAsInt();
            name = tree.get("name").isJsonNull() ? "" : tree.get("name").getAsString();
            title = tree.get("title").getAsString();
            designation = tree.get("designation").getAsString();
            unit = tree.get("unit").getAsString();
            displayValue = tree.get("displayValue").getAsString();
            rawValue = tree.get("rawValue").getAsInt();
        } catch (RuntimeException e) {
            throw new NibeUplinkRestParseException("Error parsing parameter: ", e);
        }
        return new Parameter(parameterId, name, title, designation, unit, displayValue, rawValue);
    }

    public static Parameter parseParameter(String json) {
        return parseParameter(parser.parse(json).getAsJsonObject());
    }

    public static Mode parseMode(String json) {
        JsonObject tree = (JsonObject) parser.parse(json);
        return Mode.from(tree.get("mode").getAsString());
    }
}
