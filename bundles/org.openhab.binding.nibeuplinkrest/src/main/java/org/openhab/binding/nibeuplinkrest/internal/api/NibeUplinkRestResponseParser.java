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
    private static class ZonedDateTimeDeserializer implements JsonDeserializer<ZonedDateTime> {
        @Override
        public ZonedDateTime deserialize(@Nullable JsonElement json, @Nullable Type type,
                                         @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            if (json == null)
            {
                throw new JsonParseException("null");
            }
            return ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString());
        }
    }

    /**
     * Custom deserializer to handle Mode
     */
    private static class ModeDeserializer implements JsonDeserializer<Mode> {
        @Override
        public Mode deserialize(@Nullable JsonElement json, @Nullable Type type,
                                         @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            if (json == null)
            {
                throw new JsonParseException("null");
            }
            return Mode.from(json.getAsJsonObject().get("mode").getAsString());
        }
    }

    /**
     * Custom deserializer to get system info in a nested json object
     */
    private static class NibeSystemListDeserializer implements JsonDeserializer<List<NibeSystem>> {

        @Override
        public List<NibeSystem> deserialize(@Nullable JsonElement json, @Nullable Type type,
                                            @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            if (json == null)
            {
                throw new JsonParseException("null");
            }
            JsonElement inner = json.getAsJsonObject().get("objects");

            return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                    .create().fromJson(inner, new TypeToken<List<NibeSystem>>(){}.getType());
        }
    }

    /**
     * Custom deserializer to get alarm info in a nested json object
     */
    private static class AlarmInfoDeserializer implements JsonDeserializer<AlarmInfo> {

        @Override
        public AlarmInfo deserialize(@Nullable JsonElement json, @Nullable Type type,
                                            @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            if (json == null)
            {
                throw new JsonParseException("null");
            }

            return new Gson().fromJson(json.getAsJsonObject().get("info"), AlarmInfo.class);
        }
    }

    /**
     * Custom deserializer to get a list of alarm info in a nested json object
     */
    private static class AlarmInfoListDeserializer implements JsonDeserializer<List<AlarmInfo>> {

        @Override
        public List<AlarmInfo> deserialize(@Nullable JsonElement json, @Nullable Type type,
                                     @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            if (json == null)
            {
                throw new JsonParseException("null");
            }

            JsonElement objects = json.getAsJsonObject().get("objects");

            return new Gson().fromJson(objects, new TypeToken<List<AlarmInfo>>(){}.getType());
        }
    }

    private static class SoftwareInfoDeserializer implements JsonDeserializer<SoftwareInfo> {

        @Override
        public SoftwareInfo deserialize(@Nullable JsonElement json, @Nullable Type type,
                                            @Nullable JsonDeserializationContext jsonDeserializationContext)
                throws JsonParseException {
            if (json == null)
            {
                throw new JsonParseException("null");
            }
            JsonElement current = json.getAsJsonObject().get("current").getAsJsonObject().get("name");
            JsonElement upgrade = json.getAsJsonObject().get("upgrade");
            String upgradeVersion = upgrade.isJsonNull() ? null : upgrade.getAsJsonObject().get("name").getAsString();

            return new SoftwareInfo(current.getAsString(), upgradeVersion);
        }
    }

    private static Gson gson = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
            .registerTypeAdapter(Mode.class, new ModeDeserializer())
            .registerTypeAdapter(SoftwareInfo.class, new SoftwareInfoDeserializer())
            .registerTypeAdapter(new TypeToken<List<NibeSystem>>(){}.getType(), new NibeSystemListDeserializer())
            .registerTypeAdapter(AlarmInfo.class, new AlarmInfoDeserializer())
            .registerTypeAdapter(new TypeToken<List<AlarmInfo>>(){}.getType(), new AlarmInfoListDeserializer())
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
     * Parse a json string with notification info
     * @param json
     * @return
     */
    public static List<AlarmInfo> parseAlarmInfoList(String json) {
        return gson.fromJson(json, new TypeToken<List<AlarmInfo>>(){}.getType());
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
