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

import org.eclipse.jdt.annotation.NonNullByDefault;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class TypeFactoryConstants {

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
        STRING,
        OTHER
    }

    public static final Pattern DOUBLE_PATTERN = Pattern.compile("(\\d+\\.?\\d*).*");

    public static final Map<Integer, ParameterType> STATIC_PARAMETER_TYPE_MAPPINGS = Collections.unmodifiableMap(
            Stream.of(
                    new AbstractMap.SimpleEntry<>(43416, ParameterType.COUNTER),
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
                    new AbstractMap.SimpleEntry<>(47412, ParameterType.STRING)
            ).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue))
    );

    public static final Set<String> STANDARD_CATEGORIES = Collections.unmodifiableSet(
            Stream.of("STATUS", "VENTILATION", "DEFROSTING", "SYSTEM_1", "SYSTEM_2", "SYSTEM_3", "SYSTEM_4", "SYSTEM_5",
                    "SYSTEM_6", "SYSTEM_7", "SYSTEM_8", "ADDITION", "GROUND_WATER_PUMP").collect(Collectors.toSet())
    );

    public static final String PARALLEL_ADJUST_HEAT = "parAdjustHeat";
    public static final String PARALLEL_ADJUST_COOL = "parAdjustCool";
    public static final String TARGET_TEMP_HEAT = "targetTempHeat";
    public static final String TARGET_TEMP_COOL = "targetTempCool";

    public static final Map<String, List<Integer>> HEAT_CONTROL_PARAMETERS = Collections.unmodifiableMap(
            Stream.of(
                    new AbstractMap.SimpleEntry<>(PARALLEL_ADJUST_HEAT,
                            Arrays.asList(0, 47011, 47010, 47009, 47008, 48494, 48493, 48492, 48491)),
                    new AbstractMap.SimpleEntry<>(PARALLEL_ADJUST_COOL,
                            Arrays.asList(0, 48739, 48738, 48737, 48736, 48735, 48734, 48733, 48732)),
                    new AbstractMap.SimpleEntry<>(TARGET_TEMP_HEAT,
                            Arrays.asList(0, 47398, 47397, 47396, 47395, 48683, 48682, 48681, 48680)),
                    new AbstractMap.SimpleEntry<>(TARGET_TEMP_COOL,
                            Arrays.asList(0, 48785, 48784, 48783, 48782, 48781, 48780, 48779, 48778))
            ).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue))
    );

    public static final URI CHANNEL_CONFIG = URI.create("thing-type:nibeuplinkrest:channels");
    public static final URI SYSTEM_CONFIG = URI.create("thing-type:nibeuplinkrest:system");

    public static final int NO_SCALING = 1;
    public static final int SCALE_FACTOR_TEN = 10;
    public static final int SCALE_FACTOR_HUNDRED = 100;
}
