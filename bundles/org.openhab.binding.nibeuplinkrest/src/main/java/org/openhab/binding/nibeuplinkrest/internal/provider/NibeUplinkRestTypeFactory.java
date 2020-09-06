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

import java.util.*;
import java.util.regex.Matcher;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Category;
import org.openhab.binding.nibeuplinkrest.internal.api.model.NibeSystem;
import org.openhab.binding.nibeuplinkrest.internal.api.model.Parameter;
import org.openhab.binding.nibeuplinkrest.internal.util.StringConvert;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.*;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Handles the creation of thing-types, channel-group-types and channel-types
 * based in info retreived from Nibe uplink
 *
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
    public NibeUplinkRestTypeFactory() {
    }

    /**
     * Create a new thing-type based on a system and it's categories
     * 
     * @param system
     * @param categories
     */
    public void createThingType(NibeSystem system, List<Category> categories) {
        @Nullable
        ChannelGroupType defaultControlGroup = channelGroupTypeRegistry
                .getChannelGroupType(CHANNEL_GROUP_TYPE_DEFAULT_CONTROL);

        List<ChannelGroupDefinition> groupDefinitions = new ArrayList<>();
        List<ChannelDefinition> controlChannels = new ArrayList<>();
        if (defaultControlGroup != null) {
            controlChannels.addAll(defaultControlGroup.getChannelDefinitions());
        }
        categories.forEach(c -> {
            // The system info category only contains static values, and the parameters
            // have the same id, so don't include
            if (c.getCategoryId().equals("SYSTEM_INFO")) {
                return;
            }
            // Create channel group types and channel group definitions from the categories
            ChannelGroupType groupType = createChannelGroupType(c);
            if (c.getCategoryId().equals("STATUS")) {
                groupType = withExtraStatusChannels(groupType, system);
            }
            channelGroupTypeProvider.add(groupType);
            ChannelGroupDefinition groupDefinition = createChannelGroupDefinition(groupType.getUID());
            groupDefinitions.add(groupDefinition);
        });

        // Add control channels that aren't included
        controlChannels.addAll(createHeatControlChannels(system, categories));
        ChannelGroupType controlChannelGroup = ChannelGroupTypeBuilder.instance(CHANNEL_GROUP_TYPE_CONTROL, "Control")
                .withChannelDefinitions(controlChannels).build();
        channelGroupTypeProvider.add(controlChannelGroup);
        ChannelGroupDefinition controlChannelGroupDefinition = new ChannelGroupDefinition(CHANNEL_GROUP_CONTROL_ID,
                CHANNEL_GROUP_TYPE_CONTROL);
        groupDefinitions.add(controlChannelGroupDefinition);

        // Construct the thing-type
        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID,
                system.getProductName().toLowerCase(Locale.ROOT).replaceAll("\\s", ""));
        ThingType thingType = ThingTypeBuilder.instance(thingTypeUID, system.getProductName())
                .withSupportedBridgeTypeUIDs(Collections.singletonList(THING_TYPE_APIBRIDGE.getAsString()))
                .withChannelGroupDefinitions(groupDefinitions).withConfigDescriptionURI(SYSTEM_CONFIG)
                .withLabel(system.getProductName()).withRepresentationProperty(PROPERTY_SYSTEM_ID).build();

        thingTypeProvider.addThingType(thingTypeUID, thingType);
    }

    /**
     * Create a channel group from a category, with the parameters as channels
     * 
     * @param category
     * @return
     */
    private ChannelGroupType createChannelGroupType(Category category) {
        ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(BINDING_ID,
                StringConvert.snakeCaseToCamelCase(category.getCategoryId()));

        List<ChannelDefinition> channelDefinitions = new ArrayList<>();
        category.getParameters().forEach(p -> {
            ChannelType type = createChannelType(p);
            channelTypeProvider.add(type);
            channelDefinitions.add(createChannelDefinition(type.getUID(), p));
        });

        return ChannelGroupTypeBuilder.instance(channelGroupTypeUID, StringConvert.capitalize(category.getName()))
                .withChannelDefinitions(channelDefinitions).build();
    }

    /**
     * Create a channel group definition for the thing-type
     * 
     * @param uid
     * @return
     */
    private ChannelGroupDefinition createChannelGroupDefinition(ChannelGroupTypeUID uid) {
        return new ChannelGroupDefinition(uid.getId(), uid);
    }

    /**
     * Create a channel type from a parameter
     * 
     * @param parameter
     * @return
     */
    private ChannelType createChannelType(Parameter parameter) {
        ParameterType type = getParameterType(parameter);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, type.toString().toLowerCase(Locale.ROOT));

        return ChannelTypeBuilder.state(channelTypeUID, type.toString().toLowerCase(Locale.ROOT), getItemType(type))
                .withConfigDescriptionURI(CHANNEL_CONFIG).isAdvanced(isChannelAdvanced(type))
                .withStateDescription(
                        StateDescriptionFragmentBuilder.create().withReadOnly(true).build().toStateDescription())
                .build();
    }

    /**
     * Create a channel definition for a channel group
     * 
     * @param uid
     * @param parameter
     * @return
     */
    private ChannelDefinition createChannelDefinition(ChannelTypeUID uid, Parameter parameter) {
        ParameterType type = getParameterType(parameter);
        int scalingFactor = getScalingFactor(parameter, type);

        return new ChannelDefinitionBuilder(parameter.getName(), uid)
                .withLabel(StringConvert.capitalize(parameter.getTitle()))
                .withProperties(
                        Collections.singletonMap(CHANNEL_PROPERTY_SCALING_FACTOR, Integer.toString(scalingFactor)))
                .build();
    }

    /**
     * Generate some additional channels for the status group, based on templates in XML-files
     * 
     * @return
     */
    private ChannelGroupType withExtraStatusChannels(ChannelGroupType original, NibeSystem system) {
        List<ChannelDefinition> definitions = new ArrayList<>();
        definitions.addAll(original.getChannelDefinitions());

        STATUS_CHANNELS.forEach((channelTypeID) -> {
            if ((channelTypeID.equals(CHANNEL_TYPE_VENTILATION) && !system.hasVentilation())
                    || ((channelTypeID.equals(CHANNEL_TYPE_HEATING_MEDIUM_PUMP)
                            || channelTypeID.equals(CHANNEL_TYPE_HEATING)) && !system.hasHeating())
                    || (channelTypeID.equals(CHANNEL_TYPE_HOT_WATER) && !system.hasHotWater())
                    || (channelTypeID.equals(CHANNEL_TYPE_COOLING) && !system.hasCooling())) {
                return;
            }

            @Nullable
            ChannelType channelType = channelTypeRegistry.getChannelType(channelTypeID);
            if (channelType != null) {
                ChannelDefinition definition = new ChannelDefinitionBuilder(channelTypeID.getId(), channelTypeID)
                        .withLabel(channelType.getLabel()).withDescription(channelType.getDescription()).build();
                definitions.add(definition);
            }
        });

        return ChannelGroupTypeBuilder.instance(original.getUID(), original.getLabel())
                .withChannelDefinitions(definitions).build();
    }

    /**
     * Creates control channels, based on the system's reported capabilities, based on templates
     * defined in XML-files
     * 
     * @param system
     * @param categories
     * @return
     */
    private List<ChannelDefinition> createHeatControlChannels(NibeSystem system, List<Category> categories) {
        @Nullable
        ChannelType parAdjustHeat = channelTypeRegistry.getChannelType(CHANNEL_TYPE_PARALLEL_ADJUST_HEAT);
        @Nullable
        ChannelType parAdjustCool = channelTypeRegistry.getChannelType(CHANNEL_TYPE_PARALLEL_ADJUST_COOL);
        @Nullable
        ChannelType targetTempHeat = channelTypeRegistry.getChannelType(CHANNEL_TYPE_TARGET_TEMP_HEAT);
        @Nullable
        ChannelType targetTempCool = channelTypeRegistry.getChannelType(CHANNEL_TYPE_TARGET_TEMP_COOL);
        @Nullable
        ChannelType ventilationBoost = channelTypeRegistry.getChannelType(CHANNEL_TYPE_VENTILATION_BOOST);
        @Nullable
        ChannelType hotWaterBoost = channelTypeRegistry.getChannelType(CHANNEL_TYPE_HOT_WATER_BOOST);

        List<ChannelDefinition> definitions = new ArrayList<>();

        // Add ventilation boost if the system has ventilation
        if (system.hasVentilation() && ventilationBoost != null) {
            definitions.add(new ChannelDefinitionBuilder("47260", CHANNEL_TYPE_VENTILATION_BOOST)
                    .withLabel(ventilationBoost.getLabel()).withDescription(ventilationBoost.getDescription()).build());
        }
        // Add hot water boost if the system produces hot water
        if (system.hasHotWater() && hotWaterBoost != null) {
            definitions.add(new ChannelDefinitionBuilder("48132", CHANNEL_TYPE_HOT_WATER_BOOST)
                    .withLabel(hotWaterBoost.getLabel()).withDescription(hotWaterBoost.getDescription()).build());
        }
        categories.forEach(c -> {
            // A system can support up to 8 sub-systems (e.g floor heating and radiator heating)
            // these are reported as SYSTEM_X in the categories. Add channels to control each of
            // their setpoints and heating curves
            if (c.getCategoryId().startsWith("SYSTEM") && !c.getCategoryId().equals("SYSTEM_INFO")) {
                int index = Integer.parseInt(c.getCategoryId().substring(7));
                // Add heating controls if the system has heating
                if (system.hasHeating() && parAdjustHeat != null && targetTempHeat != null) {
                    definitions.add(new ChannelDefinitionBuilder(
                            String.valueOf(HEAT_CONTROL_PARAMETERS.get(CHANNEL_PARALLEL_ADJUST_HEAT_ID).get(index)),
                            CHANNEL_TYPE_PARALLEL_ADJUST_HEAT)
                                    .withLabel(
                                            String.format(HEAT_CONTROL_CHANNEL_LABEL, index, parAdjustHeat.getLabel()))
                                    .withDescription(parAdjustHeat.getDescription()).build());
                    definitions.add(new ChannelDefinitionBuilder(
                            String.valueOf(HEAT_CONTROL_PARAMETERS.get(CHANNEL_TARGET_TEMP_HEAT_ID).get(index)),
                            CHANNEL_TYPE_TARGET_TEMP_HEAT)
                                    .withLabel(
                                            String.format(HEAT_CONTROL_CHANNEL_LABEL, index, targetTempHeat.getLabel()))
                                    .withDescription(targetTempHeat.getDescription())
                                    .withProperties(Collections.singletonMap(CHANNEL_PROPERTY_SCALING_FACTOR,
                                            Integer.toString(SCALE_FACTOR_TEN)))
                                    .build());
                }
                // Add cooling control if the system has cooling
                if (system.hasCooling() && parAdjustCool != null && targetTempCool != null) {
                    definitions.add(new ChannelDefinitionBuilder(
                            String.valueOf(HEAT_CONTROL_PARAMETERS.get(CHANNEL_PARALLEL_ADJUST_COOL_ID).get(index)),
                            CHANNEL_TYPE_PARALLEL_ADJUST_COOL)
                                    .withLabel(
                                            String.format(HEAT_CONTROL_CHANNEL_LABEL, index, parAdjustCool.getLabel()))
                                    .withDescription(parAdjustCool.getDescription()).build());
                    definitions.add(new ChannelDefinitionBuilder(
                            String.valueOf(HEAT_CONTROL_PARAMETERS.get(CHANNEL_TARGET_TEMP_COOL_ID).get(index)),
                            CHANNEL_TYPE_TARGET_TEMP_COOL)
                                    .withLabel(
                                            String.format(HEAT_CONTROL_CHANNEL_LABEL, index, targetTempCool.getLabel()))
                                    .withDescription(targetTempCool.getDescription())
                                    .withProperties(Collections.singletonMap(CHANNEL_PROPERTY_SCALING_FACTOR,
                                            Integer.toString(SCALE_FACTOR_TEN)))
                                    .build());
                }
            }
        });
        return definitions;
    }

    /**
     * Decide the parameter's typa based on
     * 1. It's reported unit
     * 2. It's reported value (if it's a boolean value)
     * 3. Static mappings if it can't be decided otherwise
     *
     * @param parameter
     * @return
     */
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
        if (STATIC_PARAMETER_TYPE_MAPPINGS.containsKey(parameter.getParameterId())) {
            return STATIC_PARAMETER_TYPE_MAPPINGS.get(parameter.getParameterId());
        }
        return ParameterType.OTHER;
    }

    /**
     * Get the parameter's scaling factor, decided by:
     * 1. Some static mappings
     * 2. Type
     * 3. Attempt to calculate based on display value and raw value
     * 
     * @param parameter
     * @param type
     * @return
     */
    private int getScalingFactor(Parameter parameter, ParameterType type) {
        if (STATIC_SCALING_FACTORS.containsKey(parameter.getParameterId())) {
            return STATIC_SCALING_FACTORS.get(parameter.getParameterId());
        }
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

    /**
     * Attempt to calculate the scaling factor by dividing the raw value with the
     * parsed display value string
     * 
     * @param parameter
     * @return
     */
    private int calculateScalingFactor(Parameter parameter) {
        double result;
        try {
            Matcher match = DOUBLE_PATTERN.matcher(parameter.getDisplayValue());
            if (match.matches()) {
                result = Double.parseDouble(match.group(1));
                return (int) Math.round(parameter.getRawValue() / result);
            }
        } catch (Exception e) {
        }
        return NO_SCALING;
    }

    /**
     * Mark some channels as advanced
     * 
     * @param type
     * @return
     */
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

    /**
     * Get the correspoding OH Item type based on the parameter type
     * 
     * @param type
     * @return
     */
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

    /**
     * Convenience method to query the thing-type provider if it has a certain thing-type
     * 
     * @param thingTypeUID
     * @return
     */
    public boolean hasThingType(ThingTypeUID thingTypeUID) {
        return thingTypeProvider.getThingType(thingTypeUID, null) != null;
    }

    /**
     * Convenience method to get access to the {@link NibeUplinkRestChannelGroupTypeProvider}
     * 
     * @return
     */
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
