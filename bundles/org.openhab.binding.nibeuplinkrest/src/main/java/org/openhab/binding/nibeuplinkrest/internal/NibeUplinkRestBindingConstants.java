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
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link NibeUplinkRestBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class NibeUplinkRestBindingConstants {

    private static final String BINDING_ID = "nibeuplinkrest";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_APIBRIDGE = new ThingTypeUID(BINDING_ID, "apibridge");
    public static final ThingTypeUID THING_TYPE_SYSTEM = new ThingTypeUID(BINDING_ID, "system");

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
    public static final String SYSTEM_LAST_ACTIVITY = "lastActivityDate";
    public static final String SYSTEM_CONNECTION_STATUS = "connectionStatus";
    public static final String SYSTEM_HAS_ALARMED = "hasAlarmed";

    private static final String BASE_URL = "https://api.nibeuplink.com/";
    private static final String OAUTH_ENDPOINT = BASE_URL + "oauth/";
    public static final String AUTH_ENDPOINT = OAUTH_ENDPOINT + "authorize";
    public static final String TOKEN_ENDPOINT = OAUTH_ENDPOINT + "token";
    public static final String API_ENDPOINT = BASE_URL + "api/v1/";
    public static final String SYSTEMS = API_ENDPOINT + "systems";
    public static final String SYSTEM_WITH_ID = SYSTEMS + "/%s";
    public static final String SYSTEM_CONFIG = SYSTEM_WITH_ID + "/config";
    public static final String SYSTEM_SOFTWARE = SYSTEM_WITH_ID + "/software";
    public static final String SCOPE = "READSYSTEM WRITESYSTEM";

    public static final String SERVLET_PATH = "/nibeuplinkconnect";
    public static final String SERVLET_IMG_PATH = SERVLET_PATH + "/img";
    public static final String SERVLET_RESOURCE_DIR = "web/";
    public static final String SERVLET_RESOURCE_IMG_DIR = SERVLET_RESOURCE_DIR + "img";
    public static final String SERVLET_TEMPLATE_INDEX = "index";
    public static final String SERVLET_TEMPLATE_INDEX_FILE = SERVLET_RESOURCE_DIR + SERVLET_TEMPLATE_INDEX + ".html";
    public static final String SERVLET_TEMPLATE_ACCOUNT = "account";
    public static final String SERVLET_TEMPLATE_ACCOUNT_FILE = SERVLET_RESOURCE_DIR + SERVLET_TEMPLATE_ACCOUNT + ".html";
}
