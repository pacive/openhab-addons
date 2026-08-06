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
package org.openhab.binding.mitsubishiheatpump.internal.pdu.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;

@NonNullByDefault
public class HorizontalVane extends AbstractType implements MitsubishiHeatpumpType {
    public static final HorizontalVane AUTO;
    public static final HorizontalVane FULL_LEFT;
    public static final HorizontalVane LEFT;
    public static final HorizontalVane CENTER;
    public static final HorizontalVane RIGHT;
    public static final HorizontalVane FULL_RIGHT;
    public static final HorizontalVane SPLIT_CENTER_LEFT;
    public static final HorizontalVane SPLIT_CENTER_RIGHT;
    public static final HorizontalVane SPLIT_LEFT_RIGHT;
    public static final HorizontalVane SPLIT_CENTER_LEFT_RIGHT;
    public static final HorizontalVane SWING;

    static {
        try {
            AUTO = new HorizontalVane(0);
            FULL_LEFT = new HorizontalVane(1);
            LEFT = new HorizontalVane(2);
            CENTER = new HorizontalVane(3);
            RIGHT = new HorizontalVane(4);
            FULL_RIGHT = new HorizontalVane(5);
            SPLIT_CENTER_LEFT = new HorizontalVane(6);
            SPLIT_CENTER_RIGHT = new HorizontalVane(7);
            SPLIT_LEFT_RIGHT = new HorizontalVane(8);
            SPLIT_CENTER_LEFT_RIGHT = new HorizontalVane(9);
            SWING = new HorizontalVane(12);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public HorizontalVane(int value) throws SerialProtocolException {
        super(value);
    }

    public HorizontalVane(byte value) throws SerialProtocolException {
        super(value & 0x0f);
    }
}
