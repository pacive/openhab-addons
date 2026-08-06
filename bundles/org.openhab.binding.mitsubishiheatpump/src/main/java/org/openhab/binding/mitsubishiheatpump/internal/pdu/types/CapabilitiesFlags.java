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
public class CapabilitiesFlags extends AbstractBitMapType implements MitsubishiHeatpumpType {
    private static final int BYTE_LENGTH = 3;

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
            HEAT_DISABLED = new CapabilitiesFlags(0x020000);
            FAN_BIT_A = new CapabilitiesFlags(0x100000);
            VERTICAL_VANE_SUPPORT = new CapabilitiesFlags(0x200000);
            VANE_SWING_SUPPORT = new CapabilitiesFlags(0x400000);
            DRY_FUNCTION_DISABLED = new CapabilitiesFlags(0x000100);
            FAN_FUNCTION_DISABLED = new CapabilitiesFlags(0x000200);
            EXTENDED_TEMP_RANGE = new CapabilitiesFlags(0x000400);
            FAN_BIT_B = new CapabilitiesFlags(0x000800);
            AUTO_FAN_SPEED_DISABLED = new CapabilitiesFlags(0x001000);
            INSTALLER_SETTINGS_SUPPORT = new CapabilitiesFlags(0x002000);
            TEST_MODE_SUPPORT = new CapabilitiesFlags(0x004000);
            DRY_TEMP_SUPPORT = new CapabilitiesFlags(0x008000);
            HAS_STATUS_DISPLAY = new CapabilitiesFlags(0x000001);
            FAN_BIT_C = new CapabilitiesFlags(0x000002);
            OUTSIDE_TEMP_REPORTING = new CapabilitiesFlags(0x000020);
        } catch (SerialProtocolException e) {
            throw new ExceptionInInitializerError("Failed to initialize");
        }
    }

    public CapabilitiesFlags(int value) throws SerialProtocolException {
        super(value, BYTE_LENGTH);
    }

    public CapabilitiesFlags(byte[] data) throws SerialProtocolException {
        super(data);
    }

    public int getFanSpeedConfiguration() {
        return ((this.getRawValue() & FAN_BIT_A.getRawValue()) >> 18)
                | ((this.getRawValue() & FAN_BIT_B.getRawValue()) >> 10)
                | ((this.getRawValue() & FAN_BIT_C.getRawValue()) >> 1);
    }

    public boolean supportsVerticalVane() {
        return isSet(VERTICAL_VANE_SUPPORT);
    }

    public boolean supportsVaneSwing() {
        return isSet(VANE_SWING_SUPPORT);
    }

    public boolean supportsEnhancedTemperature() {
        return isSet(EXTENDED_TEMP_RANGE);
    }

    public boolean supportsInstallerSettings() {
        return isSet(INSTALLER_SETTINGS_SUPPORT);
    }

    public boolean supportsTestMode() {
        return isSet(TEST_MODE_SUPPORT);
    }

    public boolean supportsDryTemp() {
        return isSet(DRY_TEMP_SUPPORT);
    }

    public boolean supportsOutsideTemp() {
        return isSet(OUTSIDE_TEMP_REPORTING);
    }

    public boolean hasStatusDisplay() {
        return isSet(HAS_STATUS_DISPLAY);
    }

    public boolean isHeatDisabled() {
        return isSet(HEAT_DISABLED);
    }

    public boolean isDryFunctionDisabled() {
        return isSet(DRY_FUNCTION_DISABLED);
    }

    public boolean isFanFunctionDisabled() {
        return isSet(FAN_FUNCTION_DISABLED);
    }

    public boolean isAutoFanSpeedDisabled() {
        return isSet(AUTO_FAN_SPEED_DISABLED);
    }
}
