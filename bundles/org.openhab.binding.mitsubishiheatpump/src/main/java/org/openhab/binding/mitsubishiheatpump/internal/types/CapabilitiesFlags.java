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
public class CapabilitiesFlags extends AbstractBitMapType implements MitsubishiHeatpumpType {
    public static final CapabilitiesFlags HEAT_DISABLED;
    public static final CapabilitiesFlags FAN_BIT_A;
    public static final CapabilitiesFlags VERTICAL_VANE_SUPPORT;
    public static final CapabilitiesFlags VANE_SWING_SUPPORT;
    public static final CapabilitiesFlags DRY_FUNCTION_DISABLED;
    public static final CapabilitiesFlags FAN_FUNCTION_DISABLED;
    public static final CapabilitiesFlags EXTENDED_TEMP_RANGE;
    public static final CapabilitiesFlags FAN_BIT_B;
    public static final CapabilitiesFlags AUTO_FAN_SPEED_DISABLED;
    public static final CapabilitiesFlags INSTALLER_SETTINGS_SUPPORT;
    public static final CapabilitiesFlags TEST_MODE_SUPPORT;
    public static final CapabilitiesFlags DRY_TEMP_SUPPORT;
    public static final CapabilitiesFlags HAS_STATUS_DISPLAY;
    public static final CapabilitiesFlags FAN_BIT_C;
    public static final CapabilitiesFlags OUTSIDE_TEMP_REPORTING;

    static {
        try {
            HEAT_DISABLED = new CapabilitiesFlags(0x020000, 3);
            FAN_BIT_A = new CapabilitiesFlags(0x100000, 3);
            VERTICAL_VANE_SUPPORT = new CapabilitiesFlags(0x200000, 3);
            VANE_SWING_SUPPORT = new CapabilitiesFlags(0x400000, 3);
            DRY_FUNCTION_DISABLED = new CapabilitiesFlags(0x000100, 3);
            FAN_FUNCTION_DISABLED = new CapabilitiesFlags(0x000200, 3);
            EXTENDED_TEMP_RANGE = new CapabilitiesFlags(0x000400, 3);
            FAN_BIT_B = new CapabilitiesFlags(0x000800, 3);
            AUTO_FAN_SPEED_DISABLED = new CapabilitiesFlags(0x001000, 3);
            INSTALLER_SETTINGS_SUPPORT = new CapabilitiesFlags(0x002000, 3);
            TEST_MODE_SUPPORT = new CapabilitiesFlags(0x004000, 3);
            DRY_TEMP_SUPPORT = new CapabilitiesFlags(0x008000, 3);
            HAS_STATUS_DISPLAY = new CapabilitiesFlags(0x000001, 3);
            FAN_BIT_C = new CapabilitiesFlags(0x000002, 3);
            OUTSIDE_TEMP_REPORTING = new CapabilitiesFlags(0x000020, 3);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public CapabilitiesFlags(int value, int byteLength) throws SerialProtocolException {
        super(value, byteLength);
    }

    public CapabilitiesFlags(byte[] data) throws SerialProtocolException {
        super(data);
    }

    public int getFanSpeedConfiguration() {
        return ((this.getRawValue() & FAN_BIT_A.getRawValue()) >> 2)
                | ((this.getRawValue() & FAN_BIT_B.getRawValue()) >> 2)
                | ((this.getRawValue() & FAN_BIT_C.getRawValue()) >> 1);
    }
}
