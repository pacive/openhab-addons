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
package org.openhab.binding.mitsubishiheatpump.internal.pdu;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.types.CapabilitiesFlags;
import org.openhab.binding.mitsubishiheatpump.internal.types.EnhancedTemperature;

@NonNullByDefault
public class BaseCapabilitiesCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = (byte) 0xc9;

    private final CapabilitiesFlags capabilitiesFlags;
    private final EnhancedTemperature minCoolSetpoint;
    private final EnhancedTemperature maxCoolSetpoint;
    private final EnhancedTemperature minHeatSetpoint;
    private final EnhancedTemperature maxHeatSetpoint;
    private final EnhancedTemperature minAutoSetpoint;
    private final EnhancedTemperature maxAutoSetpoint;

    public BaseCapabilitiesCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.capabilitiesFlags = new CapabilitiesFlags(Arrays.copyOfRange(data, 7, 10));
        this.minCoolSetpoint = new EnhancedTemperature(data[10]);
        this.maxCoolSetpoint = new EnhancedTemperature(data[11]);
        this.minHeatSetpoint = new EnhancedTemperature(data[12]);
        this.maxHeatSetpoint = new EnhancedTemperature(data[13]);
        this.minAutoSetpoint = new EnhancedTemperature(data[14]);
        this.maxAutoSetpoint = new EnhancedTemperature(data[15]);
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        System.arraycopy(capabilitiesFlags.serialize(), 0, data, 7, 3);
        data[10] = minCoolSetpoint.serialize()[0];
        data[11] = maxCoolSetpoint.serialize()[0];
        data[12] = minHeatSetpoint.serialize()[0];
        data[13] = maxHeatSetpoint.serialize()[0];
        data[14] = minAutoSetpoint.serialize()[0];
        data[15] = maxAutoSetpoint.serialize()[0];

        return data;
    }

    public CapabilitiesFlags getCapabilitiesFlags() {
        return capabilitiesFlags;
    }

    public EnhancedTemperature getMinCoolSetpoint() {
        return minCoolSetpoint;
    }

    public EnhancedTemperature getMaxCoolSetpoint() {
        return maxCoolSetpoint;
    }

    public EnhancedTemperature getMinHeatSetpoint() {
        return minHeatSetpoint;
    }

    public EnhancedTemperature getMaxHeatSetpoint() {
        return maxHeatSetpoint;
    }

    public EnhancedTemperature getMinAutoSetpoint() {
        return minAutoSetpoint;
    }

    public EnhancedTemperature getMaxAutoSetpoint() {
        return maxAutoSetpoint;
    }

    @Override
    public String toString() {
        return "BaseCapabilitiesCommand{capabilitiesFlags=" + capabilitiesFlags + ", minCoolSetpoint=" + minCoolSetpoint
                + ", maxCoolSetpoint=" + maxCoolSetpoint + ", minHeatSetpoint=" + minHeatSetpoint + ", maxHeatSetpoint="
                + maxHeatSetpoint + ", minAutoSetpoint=" + minAutoSetpoint + ", maxAutoSetpoint=" + maxAutoSetpoint
                + '}';
    }
}
