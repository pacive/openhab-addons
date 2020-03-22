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
package org.openhab.binding.nibeuplinkrest.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link NibeUplinkRestBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBindingConstants {

    public static final String BINDING_ID = "nibeuplinkrest";

    // Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_APIBRIDGE = new ThingTypeUID(BINDING_ID, "apibridge");
    public static final ThingTypeUID THING_TYPE_SYSTEM = new ThingTypeUID(BINDING_ID, "system");
    public static final ThingTypeUID THING_TYPE_THERMOSTAT = new ThingTypeUID(BINDING_ID, "thermostat");

    // Standard channel groups and channels
    public static final String CHANNEL_GROUP_STATUS_ID = "status";
    public static final String CHANNEL_GROUP_STATUS = CHANNEL_GROUP_STATUS_ID + "#";
    public static final String CHANNEL_GROUP_CONTROL_ID = "control";
    public static final String CHANNEL_GROUP_CONTROL = CHANNEL_GROUP_CONTROL_ID + "#";
    public static final String CHANNEL_COMPRESSOR_ID = "compressor";
    public static final String CHANNEL_VENTILATION_ID = "ventilation";
    public static final String CHANNEL_ADDITION_ID = "addition";
    public static final String CHANNEL_HEATING_MEDIUM_PUMP_ID = "heatingMediumPump";
    public static final String CHANNEL_HOT_WATER_ID = "hotWater";
    public static final String CHANNEL_HEATING_ID = "heating";
    public static final String CHANNEL_COOLING_ID = "cooling";
    public static final String CHANNEL_LAST_ACTIVITY_ID = "lastActivity";
    public static final String CHANNEL_HAS_ALARMED_ID = "hasAlarmed";
    public static final String CHANNEL_ALARM_INFO_ID = "alarmInfo";
    public static final String CHANNEL_MODE_ID = "mode";
    public static final String CHANNEL_SOFTWARE_UPDATE_ID = "softwareUpdate";
    public static final String CHANNEL_LATEST_SOFTWARE_ID = "latestSoftware";
    public static final String CHANNEL_VENTILATION_BOOST_ID = "ventilationBoost";
    public static final String CHANNEL_HOT_WATER_BOOST_ID = "hotWaterBoost";
    public static final String CHANNEL_PARALLEL_ADJUST_HEAT_ID = "parallelAdjustmentHeating";
    public static final String CHANNEL_PARALLEL_ADJUST_COOL_ID = "parallelAdjustmentCooling";
    public static final String CHANNEL_TARGET_TEMP_HEAT_ID = "targetTemperatureHeating";
    public static final String CHANNEL_TARGET_TEMP_COOL_ID = "targetTemperatureCooling";
    public static final String CHANNEL_COMPRESSOR = CHANNEL_GROUP_STATUS + CHANNEL_COMPRESSOR_ID;
    public static final String CHANNEL_VENTILATION = CHANNEL_GROUP_STATUS + CHANNEL_VENTILATION_ID;
    public static final String CHANNEL_ADDITION = CHANNEL_GROUP_STATUS + CHANNEL_ADDITION_ID;
    public static final String CHANNEL_HEATING_MEDIUM_PUMP = CHANNEL_GROUP_STATUS + CHANNEL_HEATING_MEDIUM_PUMP_ID;
    public static final String CHANNEL_HOT_WATER = CHANNEL_GROUP_STATUS + CHANNEL_HOT_WATER_ID;
    public static final String CHANNEL_HEATING = CHANNEL_GROUP_STATUS + CHANNEL_HEATING_ID;
    public static final String CHANNEL_COOLING = CHANNEL_GROUP_STATUS + CHANNEL_COOLING_ID;
    public static final String CHANNEL_LAST_ACTIVITY = CHANNEL_GROUP_STATUS + CHANNEL_LAST_ACTIVITY_ID;
    public static final String CHANNEL_HAS_ALARMED = CHANNEL_GROUP_STATUS + CHANNEL_HAS_ALARMED_ID;
    public static final String CHANNEL_ALARM_INFO = CHANNEL_GROUP_STATUS + CHANNEL_ALARM_INFO_ID;
    public static final String CHANNEL_SOFTWARE_UPDATE = CHANNEL_GROUP_STATUS + CHANNEL_SOFTWARE_UPDATE_ID;
    public static final String CHANNEL_LATEST_SOFTWARE = CHANNEL_GROUP_STATUS + CHANNEL_LATEST_SOFTWARE_ID;
    public static final String CHANNEL_MODE = CHANNEL_GROUP_CONTROL + CHANNEL_MODE_ID;
    public static final String CHANNEL_THERMOSTAT_CURRENT = "currentTemperature";
    public static final String CHANNEL_THERMOSTAT_TARGET = "targetTemperature";

    // Channel group type UIDs
    public static final ChannelGroupTypeUID CHANNEL_GROUP_TYPE_DEFAULT_CONTROL =
            new ChannelGroupTypeUID(BINDING_ID, "default_control");
    public static final ChannelGroupTypeUID CHANNEL_GROUP_TYPE_CONTROL =
            new ChannelGroupTypeUID(BINDING_ID, CHANNEL_GROUP_CONTROL_ID);

    // Channel type UIDs
    public static final ChannelTypeUID CHANNEL_TYPE_PARALLEL_ADJUST_HEAT =
            new ChannelTypeUID(BINDING_ID, CHANNEL_PARALLEL_ADJUST_HEAT_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_PARALLEL_ADJUST_COOL =
            new ChannelTypeUID(BINDING_ID, CHANNEL_PARALLEL_ADJUST_COOL_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_TARGET_TEMP_HEAT =
            new ChannelTypeUID(BINDING_ID, CHANNEL_TARGET_TEMP_HEAT_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_TARGET_TEMP_COOL =
            new ChannelTypeUID(BINDING_ID, CHANNEL_TARGET_TEMP_COOL_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_VENTILATION_BOOST =
            new ChannelTypeUID(BINDING_ID, CHANNEL_VENTILATION_BOOST_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_HOT_WATER_BOOST =
            new ChannelTypeUID(BINDING_ID, CHANNEL_HOT_WATER_BOOST_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_LAST_ACTIVITY =
            new ChannelTypeUID(BINDING_ID, CHANNEL_LAST_ACTIVITY_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_HAS_ALARMED =
            new ChannelTypeUID(BINDING_ID, CHANNEL_HAS_ALARMED_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_ALARM_INFO =
            new ChannelTypeUID(BINDING_ID, CHANNEL_ALARM_INFO_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_SOFTWARE_UPDATE =
            new ChannelTypeUID(BINDING_ID, CHANNEL_SOFTWARE_UPDATE_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_LATEST_SOFTWARE =
            new ChannelTypeUID(BINDING_ID, CHANNEL_LATEST_SOFTWARE_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_COMPRESSOR =
            new ChannelTypeUID(BINDING_ID, CHANNEL_COMPRESSOR_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_VENTILATION =
            new ChannelTypeUID(BINDING_ID, CHANNEL_VENTILATION_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_ADDITION =
            new ChannelTypeUID(BINDING_ID, CHANNEL_ADDITION_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_HEATING_MEDIUM_PUMP =
            new ChannelTypeUID(BINDING_ID, CHANNEL_HEATING_MEDIUM_PUMP_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_HOT_WATER =
            new ChannelTypeUID(BINDING_ID, CHANNEL_HOT_WATER_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_HEATING =
            new ChannelTypeUID(BINDING_ID, CHANNEL_HEATING_ID);
    public static final ChannelTypeUID CHANNEL_TYPE_COOLING =
            new ChannelTypeUID(BINDING_ID, CHANNEL_COOLING_ID);

    public static final String CHANNEL_PROPERTY_SCALING_FACTOR = "scalingFactor";

    // Fixed update intervals for scheduled tasks
    public static final long THERMOSTAT_UPDATE_INTERVAL = 15;
    public static final long MODE_UPDATE_INTERVAL = THERMOSTAT_UPDATE_INTERVAL;
    public static final long REQUEST_INTERVAL = 5;
    public static final int MAX_PARAMETERS_PER_REQUEST = 15;

    // Thing properties
    public static final String PROPERTY_SYSTEM_ID = "systemId";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_PRODUCT_NAME = "productName";
    public static final String PROPERTY_SECURITY_LEVEL = "securityLevel";
    public static final String PROPERTY_SERIAL_NUMBER = "serialNumber";
    public static final String PROPERTY_HAS_COOLING = "hasCooling";
    public static final String PROPERTY_HAS_HEATING = "hasHeating";
    public static final String PROPERTY_HAS_HOT_WATER = "hasHotWater";
    public static final String PROPERTY_HAS_VENTILATION = "hasVentilation";
    public static final String PROPERTY_SOFTWARE_VERSION = "softwareVersion";

    // OAuth endpoints
    private static final String BASE_URL = "https://api.nibeuplink.com/";
    private static final String OAUTH_ENDPOINT = BASE_URL + "oauth/";
    public static final String AUTH_ENDPOINT = OAUTH_ENDPOINT + "authorize";
    public static final String TOKEN_ENDPOINT = OAUTH_ENDPOINT + "token";
    public static final String SCOPE = "READSYSTEM WRITESYSTEM";

    // API endpoints
    private static final String API_ENDPOINT = BASE_URL + "api/v1/";
    public static final String API_SYSTEMS = API_ENDPOINT + "systems";
    public static final String API_SYSTEM_WITH_ID = API_SYSTEMS + "/%s";
    public static final String API_STATUS = API_SYSTEM_WITH_ID + "/status/system";
    public static final String API_NOTIFICATIONS = API_SYSTEM_WITH_ID + "/notifications";
    public static final String API_CONFIG = API_SYSTEM_WITH_ID + "/config";
    public static final String API_SOFTWARE = API_SYSTEM_WITH_ID + "/software";
    public static final String API_CATEGORIES = API_SYSTEM_WITH_ID + "/serviceinfo/categories";
    public static final String API_CATEGORY_WITH_ID = API_SYSTEM_WITH_ID + "/serviceinfo/categories/%s";
    public static final String API_PARAMETERS = API_SYSTEM_WITH_ID + "/parameters";
    public static final String API_MODE = API_SYSTEM_WITH_ID + "/smarthome/mode";
    public static final String API_THERMOSTATS = API_SYSTEM_WITH_ID + "/smarthome/thermostats";
    public static final String API_QUERY_INCLUDE_PARAMETERS = "parameters";
    public static final String API_QUERY_PARAMETER_IDS = "parameterIds";

    // Servlet paths
    public static final String SERVLET_PATH = "/nibeuplinkconnect";
    public static final String SERVLET_IMG_PATH = SERVLET_PATH + "/img";
    public static final String SERVLET_RESOURCE_DIR = "web/";
    public static final String SERVLET_RESOURCE_IMG_DIR = SERVLET_RESOURCE_DIR + "img";
    public static final String SERVLET_TEMPLATE_INDEX = "index";
    public static final String SERVLET_TEMPLATE_INDEX_FILE = SERVLET_RESOURCE_DIR + SERVLET_TEMPLATE_INDEX + ".html";
    public static final String SERVLET_TEMPLATE_ACCOUNT = "account";
    public static final String SERVLET_TEMPLATE_ACCOUNT_FILE = SERVLET_RESOURCE_DIR + SERVLET_TEMPLATE_ACCOUNT + ".html";
}
