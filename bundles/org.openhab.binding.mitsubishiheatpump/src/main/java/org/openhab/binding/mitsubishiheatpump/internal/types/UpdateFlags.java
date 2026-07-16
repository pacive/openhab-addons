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
package org.openhab.binding.mitsubishiheatpump.internal.types;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;

@NonNullByDefault
public class UpdateFlags extends AbstractBitMapType implements MitsubishiHeatpumpType {
    public static final UpdateFlags UPDATE_POWER;
    public static final UpdateFlags UPDATE_MODE;
    public static final UpdateFlags UPDATE_TEMP;
    public static final UpdateFlags UPDATE_FAN_MODE;
    public static final UpdateFlags UPDATE_VERTICAL_VANE;
    public static final UpdateFlags UPDATE_REMOTE_PROHIBIT_FLAGS;
    public static final UpdateFlags UPDATE_HORIZONTAL_VANE;

    static {
        try {
            UPDATE_POWER = new UpdateFlags(0x0100, 2);
            UPDATE_MODE = new UpdateFlags(0x0200, 2);
            UPDATE_TEMP = new UpdateFlags(0x0400, 2);
            UPDATE_FAN_MODE = new UpdateFlags(0x0800, 2);
            UPDATE_VERTICAL_VANE = new UpdateFlags(0x1000, 2);
            UPDATE_REMOTE_PROHIBIT_FLAGS = new UpdateFlags(0x4000, 2);
            UPDATE_HORIZONTAL_VANE = new UpdateFlags(0x0001, 2);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public UpdateFlags(int value, int byteLength) throws SerialProtocolException {
        super(value, byteLength);
    }

    public UpdateFlags(byte[] data) throws SerialProtocolException {
        super(data);
    }
}
