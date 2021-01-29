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

package org.openhab.binding.nibeuplinkrest.internal.provider;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.*;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;

/**
 * Constant values for use in the type factory
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class TypeFactoryConstants {

    // Internal types of parameters
    public enum ParameterType {
        TEMPERATURE,
        CURRENT,
        DEGREEMINUTES,
        BOOLEAN,
        POWER,
        AIRFLOW,
        TIME_H,
        TIME_S,
        TIME_FACTOR,
        PERCENT,
        FREQUENCY,
        COUNTER,
        NUMBER,
        STRING,
        OTHER
    }

    // Regex pattern to extract a double value - stripping unit
    public static final Pattern DOUBLE_PATTERN = Pattern.compile("(\\d+\\.?\\d*).*");

    public static final int NO_SCALING = 1;
    public static final int SCALE_FACTOR_TEN = 10;
    public static final int SCALE_FACTOR_HUNDRED = 100;

    // Map of static type mappings, for parameters that cannot be classified automatically
    public static final Map<Integer, ParameterType> STATIC_PARAMETER_TYPE_MAPPINGS = Collections.unmodifiableMap(Stream
            .of(new AbstractMap.SimpleEntry<>(43416, ParameterType.COUNTER),
                    new AbstractMap.SimpleEntry<>(43371, ParameterType.BOOLEAN),
                    new AbstractMap.SimpleEntry<>(43372, ParameterType.BOOLEAN),
                    new AbstractMap.SimpleEntry<>(43064, ParameterType.TEMPERATURE),
                    new AbstractMap.SimpleEntry<>(43065, ParameterType.TEMPERATURE),
                    new AbstractMap.SimpleEntry<>(43124, ParameterType.AIRFLOW),
                    new AbstractMap.SimpleEntry<>(40050, ParameterType.AIRFLOW),
                    new AbstractMap.SimpleEntry<>(47407, ParameterType.STRING),
                    new AbstractMap.SimpleEntry<>(47408, ParameterType.STRING),
                    new AbstractMap.SimpleEntry<>(47409, ParameterType.STRING),
                    new AbstractMap.SimpleEntry<>(47410, ParameterType.STRING),
                    new AbstractMap.SimpleEntry<>(47411, ParameterType.STRING),
                    new AbstractMap.SimpleEntry<>(47412, ParameterType.STRING))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));

    // Static scale factor mappings for parameters that need it
    public static final Map<Integer, Integer> STATIC_SCALING_FACTORS = Collections.unmodifiableMap(Stream
            .of(new AbstractMap.SimpleEntry<>(43125, SCALE_FACTOR_TEN),
                    new AbstractMap.SimpleEntry<>(43136, SCALE_FACTOR_TEN))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));

    // Parameter id mappings for the heating control channels
    public static final Map<String, List<Integer>> HEAT_CONTROL_PARAMETERS = Collections.unmodifiableMap(Stream
            .of(new AbstractMap.SimpleEntry<>(CHANNEL_PARALLEL_ADJUST_HEAT_ID,
                    Arrays.asList(0, 47011, 47010, 47009, 47008, 48494, 48493, 48492, 48491)),
                    new AbstractMap.SimpleEntry<>(CHANNEL_PARALLEL_ADJUST_COOL_ID,
                            Arrays.asList(0, 48739, 48738, 48737, 48736, 48735, 48734, 48733, 48732)),
                    new AbstractMap.SimpleEntry<>(CHANNEL_TARGET_TEMP_HEAT_ID,
                            Arrays.asList(0, 47398, 47397, 47396, 47395, 48683, 48682, 48681, 48680)),
                    new AbstractMap.SimpleEntry<>(CHANNEL_TARGET_TEMP_COOL_ID,
                            Arrays.asList(0, 48785, 48784, 48783, 48782, 48781, 48780, 48779, 48778)))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));

    public static final String HEAT_CONTROL_CHANNEL_LABEL = "System %s %s";

    public static final List<ChannelTypeUID> STATUS_CHANNELS = Arrays.asList(CHANNEL_TYPE_COMPRESSOR,
            CHANNEL_TYPE_VENTILATION, CHANNEL_TYPE_ADDITION, CHANNEL_TYPE_HEATING_MEDIUM_PUMP, CHANNEL_TYPE_HOT_WATER,
            CHANNEL_TYPE_HEATING, CHANNEL_TYPE_COOLING, CHANNEL_TYPE_LAST_ACTIVITY, CHANNEL_TYPE_HAS_ALARMED,
            CHANNEL_TYPE_ALARM_INFO, CHANNEL_TYPE_SOFTWARE_UPDATE, CHANNEL_TYPE_LATEST_SOFTWARE);

    public static final Set<String> STANDARD_CHANNEL_GROUPS = Stream.of("STATUS", "SYSTEM_1", "SYSTEM_2", "SYSTEM_3",
            "SYSTEM_4", "SYSTEM_5", "SYSTEM_6", "SYSTEM_7", "SYSTEM_8", "CONTROL").collect(Collectors.toSet());

    // References to XML defined config descriptions
    public static final URI CHANNEL_CONFIG = URI.create("channel-type:nibeuplinkrest:channels");
    public static final URI SYSTEM_CONFIG = URI.create("thing-type:nibeuplinkrest:system");
}
