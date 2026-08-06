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
package org.openhab.binding.mitsubishiheatpump.internal.pdu.command;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.mitsubishiheatpump.internal.exception.SerialProtocolException;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.EnhancedTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.LegacyHPRoomTemperature;
import org.openhab.binding.mitsubishiheatpump.internal.pdu.types.TimeType;

@NonNullByDefault
public class GetTemperaturesCommand implements MitsubishiHeatpumpCommand {
    public static final byte COMMAND_ID = 0x03;

    private final LegacyHPRoomTemperature legacyHPRoomTemperature;
    private final EnhancedTemperature outdoorUnitTemperature;
    private final EnhancedTemperature currentTemperature;
    private final TimeType runtime;

    public GetTemperaturesCommand(byte[] data) throws SerialProtocolException {
        if (data.length != DEFAULT_LENGTH) {
            throw new SerialProtocolException("Data of unexpected length");
        }
        this.legacyHPRoomTemperature = new LegacyHPRoomTemperature(data[3]);
        this.outdoorUnitTemperature = new EnhancedTemperature(data[5]);
        this.currentTemperature = new EnhancedTemperature(data[6]);
        this.runtime = new TimeType(Arrays.copyOfRange(data, 11, 14));
    }

    public byte getId() {
        return COMMAND_ID;
    }

    public byte[] serialize() {
        byte[] data = new byte[DEFAULT_LENGTH];
        data[0] = COMMAND_ID;
        data[3] = legacyHPRoomTemperature.serialize()[0];
        data[5] = outdoorUnitTemperature.serialize()[0];
        data[6] = currentTemperature.serialize()[0];
        System.arraycopy(runtime.serialize(), 0, data, 11, 3);

        return data;
    }

    public LegacyHPRoomTemperature getLegacyRoomTemperature() {
        return legacyHPRoomTemperature;
    }

    public EnhancedTemperature getOutdoorUnitTemperature() {
        return outdoorUnitTemperature;
    }

    public EnhancedTemperature getCurrentTemperature() {
        return currentTemperature;
    }

    public TimeType getRuntime() {
        return runtime;
    }

    @Override
    public String toString() {
        return "GetTemperaturesCommand{legacyRoomTemperature=" + legacyHPRoomTemperature + ", outdoorUnitTemperature="
                + outdoorUnitTemperature + ", currentTemperature=" + currentTemperature + ", runtime=" + runtime + '}';
    }
}
