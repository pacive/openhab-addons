/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.zaptec.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link ZaptecBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class ZaptecBindingConstants {

    private static final String BINDING_ID = "zaptec";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ACCOUNT = new ThingTypeUID(BINDING_ID, "account");
    public static final ThingTypeUID THING_TYPE_INSTALLATION = new ThingTypeUID(BINDING_ID, "installation");
    public static final ThingTypeUID THING_TYPE_CHARGER = new ThingTypeUID(BINDING_ID, "charger");

    // List of all Channel ids
    public static final String API_URL = "https://api.zaptec.com/";
    public static final String TOKEN_ENDPOINT = API_URL + "oauth/token";
    public static final String API_BASE = API_URL + "api/";
    public static final String API_CONSTANTS = API_BASE + "constants";

    public static final String API_CHARGERS = API_BASE + "chargers";

    public static final String API_INSTALLATION = API_BASE + "installation";

    public static final String SCOPE = "offline_access";
    public static final String CONTENT_TYPE = "application/json";
}
