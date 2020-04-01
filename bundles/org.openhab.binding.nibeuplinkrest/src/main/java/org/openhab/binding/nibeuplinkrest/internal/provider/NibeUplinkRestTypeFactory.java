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

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.*;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Parameter;
import org.openhab.binding.nibeuplinkrest.internal.util.StringConvert;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.net.URI;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openhab.binding.nibeuplinkrest.internal.NibeUplinkRestBindingConstants.BINDING_ID;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@Component(immediate = true, service = { NibeUplinkRestTypeFactory.class })
public class NibeUplinkRestTypeFactory {

    private enum ParameterType {
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

    private static final Pattern DOUBLE_PATTERN = Pattern.compile("(\\d+\\.?\\d*).*");

    private static final Map<Integer, ParameterType> STATIC_PARAMETER_TYPE_MAPPINGS = Collections.unmodifiableMap(
            Stream.of(
                    new SimpleEntry<>(43416, ParameterType.COUNTER),
                    new SimpleEntry<>(43371, ParameterType.BOOLEAN),
                    new SimpleEntry<>(43372, ParameterType.BOOLEAN),
                    new SimpleEntry<>(43064, ParameterType.TEMPERATURE),
                    new SimpleEntry<>(43065, ParameterType.TEMPERATURE),
                    new SimpleEntry<>(43124, ParameterType.AIRFLOW),
                    new SimpleEntry<>(40050, ParameterType.AIRFLOW),
                    new SimpleEntry<>(47407, ParameterType.STRING),
                    new SimpleEntry<>(47408, ParameterType.STRING),
                    new SimpleEntry<>(47409, ParameterType.STRING),
                    new SimpleEntry<>(47410, ParameterType.STRING),
                    new SimpleEntry<>(47411, ParameterType.STRING),
                    new SimpleEntry<>(47412, ParameterType.STRING)
            ).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))
    );

    private static final Set<String> STANDARD_CATEGORIES = Collections.unmodifiableSet(
            Stream.of("STATUS", "VENTILATION", "DEFROSTING", "SYSTEM_1", "SYSTEM_2", "SYSTEM_3", "SYSTEM_4", "SYSTEM_5",
                    "SYSTEM_6", "SYSTEM_7", "SYSTEM_8", "ADDITION", "GROUND_WATER_PUMP").collect(Collectors.toSet())
    );

    private static final URI CHANNEL_CONFIG = URI.create("thing-type:nibeuplinkrest:channels");
    private static final URI SYSTEM_CONFIG = URI.create("thing-type:nibeuplinkrest:system");

    private static final int NO_SCALING = 1;
    private static final int SCALE_FACTOR_TEN = 10;
    private static final int SCALE_FACTOR_HUNDRED = 100;

    private @NonNullByDefault({}) NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider;
    private @NonNullByDefault({}) NibeUplinkRestChannelTypeProvider channelTypeProvider;
    private @NonNullByDefault({}) NibeUplinkRestThingTypeProvider thingTypeProvider;

    public NibeUplinkRestTypeFactory() {}

    public ThingTypeUID createThingType(NibeSystem system, List<Category> categories) {
        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID,
                StringUtils.deleteWhitespace(system.getProductName()).toLowerCase(Locale.ROOT));
        List<ChannelGroupDefinition> groupDefinitions = new ArrayList<>();

        categories.forEach(c -> {
            if (c.getCategoryId().equals("SYSTEM_INFO")) {
                return;
            }
            ChannelGroupType groupType = createChannelGroupType(c);
            channelGroupTypeProvider.add(groupType);
            ChannelGroupDefinition groupDefinition = createChannelGroupDefinition(groupType.getUID());
            groupDefinitions.add(groupDefinition);
        });

        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, system.getProductName())
                .withSupportedBridgeTypeUIDs(Collections.singletonList("apibridge"))
                .withChannelGroupDefinitions(groupDefinitions)
                .withConfigDescriptionURI(SYSTEM_CONFIG)
                .withLabel("System")
                .withRepresentationProperty("systemId")
                .build();

        thingTypeProvider.addThingType(thingTypeUID, thingType);

        return thingTypeUID;
    }

    private ChannelGroupType createChannelGroupType(Category category) {
        ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(BINDING_ID,
                StringConvert.snakeCaseToCamelCase(category.getCategoryId()));

        List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        category.getParameters().forEach(p -> {
                    ChannelType type = createChannelType(p);
                    channelTypeProvider.add(type);
                    channelDefinitions.add(createChannelDefinition(type.getUID(), p));
                });


        ChannelGroupType channelGroupType = ChannelGroupTypeBuilder.instance(channelGroupTypeUID, category.getName())
                .withChannelDefinitions(channelDefinitions)
                .isAdvanced(isCategoryAdvanced(category))
                .build();

        return channelGroupType;
    }

    private ChannelGroupDefinition createChannelGroupDefinition(ChannelGroupTypeUID uid) {
        return new ChannelGroupDefinition(uid.getId(), uid);
    }

    private ChannelType createChannelType(Parameter parameter) {
        ParameterType type = getParameterType(parameter);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, type.toString().toLowerCase(Locale.ROOT));

        ChannelType channelType = ChannelTypeBuilder.state(channelTypeUID, parameter.getTitle(), getItemType(type))
                .withConfigDescriptionURI(CHANNEL_CONFIG)
                .build();

        return channelType;
    }

    private ChannelDefinition createChannelDefinition(ChannelTypeUID uid, Parameter parameter) {
        ParameterType type = getParameterType(parameter);
        int scalingFactor = getScalingFactor(parameter, type);


        return new ChannelDefinitionBuilder(parameter.getName(), uid)
                .withLabel(parameter.getTitle())
                .withProperties(Collections.singletonMap("scalingFactor", Integer.toString(scalingFactor)))
                .build();
    }

    private ParameterType getParameterType(Parameter parameter) {
        switch (parameter.getUnit()) {
            case "°C":
                return ParameterType.TEMPERATURE;
            case "A":
                return ParameterType.CURRENT;
            case "DM":
                return ParameterType.DEGREEMINUTES;
            case "%":
                return ParameterType.PERCENT;
            case "h":
                return ParameterType.TIME_H;
            case "s":
                return ParameterType.TIME_S;
            case "Hz":
                return ParameterType.FREQUENCY;
            case "kWh":
                return ParameterType.TIME_FACTOR;
        }
        if (parameter.getDisplayValue().equals("yes") || parameter.getDisplayValue().equals("no")) {
            return ParameterType.BOOLEAN;
        }
        if (STATIC_PARAMETER_TYPE_MAPPINGS.containsKey(parameter.getParameterId()))
        {
            return STATIC_PARAMETER_TYPE_MAPPINGS.get(parameter.getParameterId());
        }
        return ParameterType.OTHER;
    }

    private int getScalingFactor(Parameter parameter, ParameterType type) {
        switch (type) {
            case TEMPERATURE:
            case DEGREEMINUTES:
            case AIRFLOW:
                return SCALE_FACTOR_TEN;
            case TIME_FACTOR:
            case POWER:
                return SCALE_FACTOR_HUNDRED;
            case CURRENT:
            case TIME_H:
            case FREQUENCY:
            case PERCENT:
                return calculateScalingFactor(parameter);
            default:
                return NO_SCALING;
        }
    }

    private int calculateScalingFactor(Parameter parameter) {
        Double result;
        try {
            Matcher match = DOUBLE_PATTERN.matcher(parameter.getDisplayValue());
            if (match.matches()) {
                result = Double.valueOf(match.group(1));
                return (int) Math.round(parameter.getRawValue() / result);
            }
        } catch (Exception e) { }
        return NO_SCALING;
    }

    private boolean isCategoryAdvanced(Category category) {
        return !STANDARD_CATEGORIES.contains(category.getCategoryId());
    }

    private String getItemType(ParameterType type) {
        switch (type) {
            case STRING:
                return "String";
            case BOOLEAN:
                return "Switch";
            default:
                return "Number";
        }
    }

    public boolean hasThingType(ThingTypeUID thingTypeUID) {
        return thingTypeProvider.getThingType(thingTypeUID, null) != null;
    }

    @Activate
    public void activate() {}

    @Reference
    protected void setChannelGroupTypeProvider(NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProvider = channelGroupTypeProvider;
    }

    protected void unsetChannelGroupTypeProvider(NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider) {
        this.channelGroupTypeProvider = null;
    }

    @Reference
    protected void setChannelTypeProvider(NibeUplinkRestChannelTypeProvider channelTypeProvider) {
        this.channelTypeProvider = channelTypeProvider;
    }

    protected void unsetChannelTypeProvider(NibeUplinkRestChannelTypeProvider channelTypeProvider) {
        this.channelTypeProvider = null;
    }

    @Reference
    protected void setThingTypeProvider(NibeUplinkRestThingTypeProvider thingTypeProvider) {
        this.thingTypeProvider = thingTypeProvider;
    }

    protected void unsetThingTypeProvider(NibeUplinkRestThingTypeProvider thingTypeProvider) {
        this.thingTypeProvider = null;
    }
}
