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
import static org.openhab.binding.nibeuplinkrest.internal.provider.TypeFactoryConstants.*;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.type.*;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Parameter;
import org.openhab.binding.nibeuplinkrest.internal.util.StringConvert;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Anders Alfredsson - Initial contribution
 */
@Component(immediate = true, service = { NibeUplinkRestTypeFactory.class })
@NonNullByDefault
public class NibeUplinkRestTypeFactory {

    private @NonNullByDefault({}) NibeUplinkRestChannelGroupTypeProvider channelGroupTypeProvider;
    private @NonNullByDefault({}) NibeUplinkRestChannelTypeProvider channelTypeProvider;
    private @NonNullByDefault({}) NibeUplinkRestThingTypeProvider thingTypeProvider;
    private @NonNullByDefault({}) ChannelGroupTypeRegistry channelGroupTypeRegistry;
    private @NonNullByDefault({}) ChannelTypeRegistry channelTypeRegistry;

    @Activate
    public NibeUplinkRestTypeFactory() {}

    public void createThingType(NibeSystem system, List<Category> categories) {
        ChannelGroupType defaultControlGroup = channelGroupTypeRegistry.getChannelGroupType(
                CHANNEL_GROUP_TYPE_DEFAULT_CONTROL);

        List<ChannelGroupDefinition> groupDefinitions = new ArrayList<>();
        List<ChannelDefinition> controlChannels = new ArrayList<>();
        if (defaultControlGroup != null) {
            controlChannels.addAll(defaultControlGroup.getChannelDefinitions());
        }
        categories.forEach(c -> {
            if (c.getCategoryId().equals("SYSTEM_INFO")) {
                return;
            }
            if (c.getCategoryId().startsWith("SYSTEM")) {
                controlChannels.addAll(createHeatControlChannels(c.getCategoryId().substring(7)));
            }
            ChannelGroupType groupType = createChannelGroupType(c);
            channelGroupTypeProvider.add(groupType);
            ChannelGroupDefinition groupDefinition = createChannelGroupDefinition(groupType.getUID());
            groupDefinitions.add(groupDefinition);
        });

        ChannelGroupType controlChannelGroup = ChannelGroupTypeBuilder.instance(CHANNEL_GROUP_TYPE_CONTROL, "Control")
                .withChannelDefinitions(controlChannels)
                .build();

        channelGroupTypeProvider.add(controlChannelGroup);
        ChannelGroupDefinition controlChannelGroupDefinition = new ChannelGroupDefinition(CHANNEL_GROUP_CONTROL_ID,
                CHANNEL_GROUP_TYPE_CONTROL);
        groupDefinitions.add(controlChannelGroupDefinition);

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID,
                StringUtils.deleteWhitespace(system.getProductName()).toLowerCase(Locale.ROOT));

        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, system.getProductName())
                .withSupportedBridgeTypeUIDs(Collections.singletonList(THING_TYPE_APIBRIDGE.getAsString()))
                .withChannelGroupDefinitions(groupDefinitions)
                .withConfigDescriptionURI(SYSTEM_CONFIG)
                .withLabel(system.getProductName())
                .withRepresentationProperty(PROPERTY_SYSTEM_ID)
                .build();

        thingTypeProvider.addThingType(thingTypeUID, thingType);
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

        if (category.getCategoryId().equals("STATUS")) {
            channelDefinitions.add(
                    new ChannelDefinitionBuilder(CHANNEL_LAST_ACTIVITY_ID, CHANNEL_TYPE_LAST_ACTIVITY).build()
            );
            channelDefinitions.add(
                    new ChannelDefinitionBuilder(CHANNEL_HAS_ALARMED_ID, CHANNEL_TYPE_HAS_ALARMED).build()
            );
        }

        return ChannelGroupTypeBuilder.instance(channelGroupTypeUID,
                StringUtils.capitalize(category.getName()))
                .withChannelDefinitions(channelDefinitions)
                .isAdvanced(isCategoryAdvanced(category.getCategoryId()))
                .build();
    }

    private ChannelGroupDefinition createChannelGroupDefinition(ChannelGroupTypeUID uid) {
        return new ChannelGroupDefinition(uid.getId(), uid);
    }

    private ChannelType createChannelType(Parameter parameter) {
        ParameterType type = getParameterType(parameter);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, type.toString().toLowerCase(Locale.ROOT));

        return ChannelTypeBuilder.state(channelTypeUID,
                type.toString().toLowerCase(Locale.ROOT), getItemType(type))
                .withConfigDescriptionURI(CHANNEL_CONFIG)
                .isAdvanced(isChannelAdvanced(type))
                .withStateDescription(StateDescriptionFragmentBuilder.create()
                        .withReadOnly(true).build().toStateDescription())
                .build();
    }

    private ChannelDefinition createChannelDefinition(ChannelTypeUID uid, Parameter parameter) {
        ParameterType type = getParameterType(parameter);
        int scalingFactor = getScalingFactor(parameter, type);

        return new ChannelDefinitionBuilder(parameter.getName(), uid)
                .withLabel(StringUtils.capitalize(parameter.getTitle()))
                .withProperties(Collections.singletonMap(CHANNEL_PROPERTY_SCALING_FACTOR, Integer.toString(scalingFactor)))
                .build();
    }

    private List<ChannelDefinition> createHeatControlChannels(String index) {
        ChannelType parAdjustHeatType = channelTypeRegistry.getChannelType(CHANNEL_TYPE_PARALLEL_ADJUST_HEAT);
        ChannelType parAdjustCoolType = channelTypeRegistry.getChannelType(CHANNEL_TYPE_PARALLEL_ADJUST_COOL);
        ChannelType targetTempHeatType = channelTypeRegistry.getChannelType(CHANNEL_TYPE_TARGET_TEMP_HEAT);
        ChannelType targetTempCoolType = channelTypeRegistry.getChannelType(CHANNEL_TYPE_TARGET_TEMP_COOL);

        if (parAdjustHeatType != null && parAdjustCoolType != null &&
                targetTempHeatType != null && targetTempCoolType != null) {
            ChannelDefinition parAdjustHeat = new ChannelDefinitionBuilder(
                    String.valueOf(HEAT_CONTROL_PARAMETERS.get(PARALLEL_ADJUST_HEAT)
                            .get(Integer.parseInt(index))), CHANNEL_TYPE_PARALLEL_ADJUST_HEAT)
                    .withLabel("System " + index + " " + parAdjustHeatType.getLabel())
                    .withDescription(parAdjustHeatType.getDescription())
                    .build();
            ChannelDefinition parAdjustCool = new ChannelDefinitionBuilder(
                    String.valueOf(HEAT_CONTROL_PARAMETERS.get(PARALLEL_ADJUST_COOL)
                            .get(Integer.parseInt(index))), CHANNEL_TYPE_PARALLEL_ADJUST_COOL)
                    .withLabel("System " + index + " " + parAdjustCoolType.getLabel())
                    .withDescription(parAdjustCoolType.getDescription())
                    .build();
            ChannelDefinition targetTempHeat = new ChannelDefinitionBuilder(
                    String.valueOf(HEAT_CONTROL_PARAMETERS.get(TARGET_TEMP_HEAT)
                            .get(Integer.parseInt(index))), CHANNEL_TYPE_TARGET_TEMP_HEAT)
                    .withLabel("System " + index + " " + targetTempHeatType.getLabel())
                    .withDescription(targetTempHeatType.getDescription())
                    .build();
            ChannelDefinition targetTempCool = new ChannelDefinitionBuilder(
                    String.valueOf(HEAT_CONTROL_PARAMETERS.get(TARGET_TEMP_COOL)
                            .get(Integer.parseInt(index))), CHANNEL_TYPE_TARGET_TEMP_COOL)
                    .withLabel("System " + index + " " + targetTempCoolType.getLabel())
                    .withDescription(targetTempCoolType.getDescription())
                    .build();

            return Stream.of(parAdjustHeat, parAdjustCool, targetTempHeat, targetTempCool).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
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
            case "kW":
                return ParameterType.POWER;
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
        double result;
        try {
            Matcher match = DOUBLE_PATTERN.matcher(parameter.getDisplayValue());
            if (match.matches()) {
                result = Double.parseDouble(match.group(1));
                return (int) Math.round(parameter.getRawValue() / result);
            }
        } catch (Exception e) { }
        return NO_SCALING;
    }

    private boolean isCategoryAdvanced(String categoryId) {
        return !STANDARD_CATEGORIES.contains(categoryId);
    }

    private boolean isChannelAdvanced(ParameterType type) {
        switch (type) {
            case CURRENT:
            case STRING:
            case TIME_FACTOR:
                return true;
            default:
                return false;
        }
    }

    private String getItemType(ParameterType type) {
        switch (type) {
            case STRING:
                return CoreItemFactory.STRING;
            case BOOLEAN:
                return CoreItemFactory.SWITCH;
            default:
                return CoreItemFactory.NUMBER;
        }
    }

    public boolean hasThingType(ThingTypeUID thingTypeUID) {
        return thingTypeProvider.getThingType(thingTypeUID, null) != null;
    }

    public NibeUplinkRestChannelGroupTypeProvider getGroupTypeProvider() {
        return channelGroupTypeProvider;
    }

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

    @Reference
    protected void setChannelGroupTypeRegistry(ChannelGroupTypeRegistry channelGroupTypeRegistry) {
        this.channelGroupTypeRegistry = channelGroupTypeRegistry;
    }

    protected void unsetChannelGroupTypeRegistry(ChannelGroupTypeRegistry channelGroupTypeRegistry) {
        this.channelGroupTypeRegistry = null;
    }

    @Reference
    protected void setChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = channelTypeRegistry;
    }

    protected void unsetChannelTypeRegistry(ChannelTypeRegistry channelTypeRegistry) {
        this.channelTypeRegistry = null;
    }
}
