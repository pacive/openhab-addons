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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nibeuplinkrest.internal.api.model.*;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestResponseParser {

    /**
     * Custom deserializer to handle ZonedDateTime
     */
    @NonNullByDefault
    private static class ZonedDateTimeDeserializer implements JsonDeserializer<ZonedDateTime> {
        @Override
        public ZonedDateTime deserialize(@Nullable JsonElement json, @Nullable Type type,
                                         @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            return ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString());
        }
    }

    /**
     * Custom deserializer to get system info in a nested json object
     */
    @NonNullByDefault
    private static class NibeSystemListDeserializer implements JsonDeserializer<List<NibeSystem>> {

        @Override
        public List<NibeSystem> deserialize(@Nullable JsonElement json, @Nullable Type type,
                                            @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            JsonElement inner = json.getAsJsonObject().get("objects");

            return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                    .create().fromJson(inner, new TypeToken<List<NibeSystem>>(){}.getType());
        }
    }

    private static Gson gson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
            .registerTypeAdapter(new TypeToken<List<NibeSystem>>(){}.getType(), new NibeSystemListDeserializer())
            .create();

    /**
     * Parse a json string with system info
     * @param json
     * @return
     */
    public static NibeSystem parseSystem(String json) {
        return gson.fromJson(json, NibeSystem.class);
    }

    /**
     * Parse a json string with a list of systems
     * @param json
     * @return
     */
    public static List<NibeSystem> parseSystemList(String json) {
        return gson.fromJson(json, new TypeToken<List<NibeSystem>>(){}.getType());
    }

    /**
     * Parse a json string with system config information
     * @param json
     * @return
     */
    public static SystemConfig parseSystemConfig(String json) {
        return gson.fromJson(json, SystemConfig.class);
    }

    /**
     * Parse a json string with software info
     * @param json
     * @return
     */
    public static SoftwareInfo parseSoftwareInfo(String json) {
        return gson.fromJson(json, SoftwareInfo.class);
    }

    /**
     * Parse a json string with a list of categories
     * @param json
     * @return
     */
    public static List<Category> parseCategoryList(String json) {
        return gson.fromJson(json, new TypeToken<List<Category>>(){}.getType());
    }

    /**
     * Parse a json string with a list of parameters
     * @param json
     * @return
     */
    public static List<Parameter> parseParameterList(String json) {
        return gson.fromJson(json, new TypeToken<List<Parameter>>(){}.getType());
    }

    /**
     * Parse a json string with info on the mode of the system
     * @param json
     * @return
     */
    public static Mode parseMode(String json) {
        return gson.fromJson(json, Mode.class);
    }
}
