/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.mitsubishiheatpump.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.util.Util;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link MitsubishiHeatpumpBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Anders Alfredsson - Initial contribution
 */
@NonNullByDefault
public class MitsubishiHeatpumpBindingConstants {

    private static final String BINDING_ID = "mitsubishiheatpump";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_HEATPUMP = new ThingTypeUID(BINDING_ID, "heatpump");

    // List of all Channel ids
    public static final String CHANNEL_POWER = "power";
    public static final String CHANNEL_MODE = "mode";
    public static final String CHANNEL_SETPOINT = "setpoint";
    public static final String CHANNEL_FAN = "fan";
    public static final String CHANNEL_VERTICAL_VANE = "verticalVane";
    public static final String CHANNEL_HORIZONTAL_VANE = "horizontalVane";
    public static final String CHANNEL_INDOOR_TEMP = "indoorTemp";
    public static final String CHANNEL_OUTDOOR_TEMP = "outdoorTemp";
    public static final String CHANNEL_REMOTE_TEMP = "remoteTemp";
    public static final String CHANNEL_RUNTIME = "runtime";

    // Protocol specific constants
    public static final byte SYNC = (byte) 0xfc;
    public static final byte[] PROTOCOL_IDENTIFIER = Util.intToBytes(0x0130, 2);

    public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ESV>";
    public static final String XML_COMMAND_HEADER = "<CSV>";
    public static final String XML_CONNECT_TAG = "<CONNECT>ON</CONNECT>";
    public static final String XML_CODE_HEADER = "<CODE><VALUE>";
    public static final String XML_CODE_FOOTER = "</VALUE></CODE>";
    public static final String XML_COMMAND_FOOTER = "</CSV>";
    public static final String XML_FOOTER = "</ESV>";
}
