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

    // List of all Channel ids
    //public static final String CHANNEL_1 = "channel1";

    private static final String BASE_URL = "https://api.nibeuplink.com/";
    private static final String OAUTH_ENDPOINT = BASE_URL + "oauth/";
    public static final String AUTH_ENDPOINT = OAUTH_ENDPOINT + "authorize";
    public static final String TOKEN_ENDPOINT = OAUTH_ENDPOINT + "token";
    private static final String API_ENDPOINT = BASE_URL + "api/v1/";
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
